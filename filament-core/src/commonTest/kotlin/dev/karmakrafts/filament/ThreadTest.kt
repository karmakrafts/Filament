/*
 * Copyright 2026 Karma Krafts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.filament

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalAtomicApi::class)
class ThreadTest {
    @Test
    fun `Create and join`() {
        val calls = AtomicInt(0)
        val thread = Thread { calls.fetchAndIncrement() }
        thread.join()
        assertEquals(1, calls.load())
        assertFalse(thread.isAlive)
        assertFalse(thread.isDetached)
        assertEquals(Thread.NO_AFFINITY, thread.affinity)
    }

    @Test
    fun `Check current thread ID`() {
        val id = Thread.id
        assertNotEquals(0UL, id)
        assertEquals(id, Thread.id)
    }

    @Test
    fun `Spin loop hint preserves current thread ID`() {
        val id = Thread.id
        Thread.spinLoopHint()
        assertEquals(id, Thread.id)
    }

    @Test
    fun `Spin loop hint can be called repeatedly`() {
        val id = Thread.id
        repeat(10_000) {
            Thread.spinLoopHint()
        }
        assertEquals(id, Thread.id)
    }

    @Test
    fun `Spin loop hint can wait for an atomic signal`() {
        val ready = AtomicBoolean(false)
        val release = AtomicBoolean(false)
        val observed = AtomicBoolean(false)
        val thread = Thread {
            val start = TimeSource.Monotonic.markNow()
            do {
                Thread.spinLoopHint()
                ready.store(true)
            }
            while (!release.load() && start.elapsedNow() < 5.seconds)
            observed.store(release.load())
        }
        try {
            val start = TimeSource.Monotonic.markNow()
            while (!ready.load() && start.elapsedNow() < 5.seconds) {
                Thread.spinLoopHint()
            }
            assertTrue(ready.load(), "Worker did not enter the spin loop")
        }
        finally {
            release.store(true)
            thread.join()
        }
        assertTrue(observed.load(), "Worker did not observe the atomic signal")
    }

    @Test
    fun `Check separate thread ID`() {
        val parentId = Thread.id
        val id = Thread<ULong> { Thread.id }.awaitResult()
        assertNotEquals(0UL, id)
        assertNotEquals(parentId, id)
        assertEquals(parentId, Thread.id)
    }

    @Test
    fun `Set and get thread affinity`() {
        val thread = Thread<Int>(affinity = 0) { Thread.current().affinity }
        val affinity = thread.awaitResult()
        assertEquals(0, thread.affinity)
        assertEquals(0, affinity)
    }

    @Test
    fun `Creating a thread preserves the parent affinity`() {
        val affinities = Thread<Pair<Int, Int>> {
            val before = Thread.current().affinity
            Thread(affinity = 0) {}.join()
            before to Thread.current().affinity
        }.awaitResult()
        assertEquals(affinities.first, affinities.second)
    }

    @Test
    fun `Thread with result`() {
        val result = Thread<String> {
            "Hello, World!"
        }.awaitResult()
        assertEquals("Hello, World!", result)
    }

    @Test
    fun `Thread is alive until released and joined`() {
        val ready = AtomicBoolean(false)
        val release = AtomicBoolean(false)
        val observed = AtomicBoolean(false)
        val thread = Thread {
            ready.store(true)
            val start = TimeSource.Monotonic.markNow()
            while (!release.load() && start.elapsedNow() < 5.seconds) {
                Thread.yield()
            }
            observed.store(release.load())
        }
        try {
            val start = TimeSource.Monotonic.markNow()
            while (!ready.load() && start.elapsedNow() < 5.seconds) {
                Thread.yield()
            }
            assertTrue(ready.load(), "Worker did not start")
            assertTrue(thread.isAlive)
            assertFalse(thread.isDetached)
        }
        finally {
            release.store(true)
            thread.join()
        }
        assertTrue(observed.load(), "Worker did not observe the release signal")
        assertFalse(thread.isAlive)
    }

    @Test
    fun `Joining a thread repeatedly does not execute it again`() {
        val calls = AtomicInt(0)
        val thread = Thread { calls.fetchAndIncrement() }
        repeat(3) { thread.join() }
        assertEquals(1, calls.load())
        assertFalse(thread.isAlive)
    }

    @Test
    fun `Current thread reports that it is alive`() {
        assertTrue(Thread.current().isAlive)
        assertTrue(Thread<Boolean> { Thread.current().isAlive }.awaitResult())
    }

    @Test
    fun `Thread names are local to the current thread`() {
        val originalName = Thread.name
        try {
            Thread.name = "parent-test"
            val name = Thread<String> {
                Thread.name = "worker-test"
                Thread.name
            }.awaitResult()
            assertEquals("worker-test", name)
            assertEquals("parent-test", Thread.name)
        }
        finally {
            Thread.name = originalName
        }
        assertEquals(originalName, Thread.name)
    }

    @Test
    fun `Yield preserves current thread ID`() {
        val id = Thread.id
        repeat(100) { Thread.yield() }
        assertEquals(id, Thread.id)
    }

    @Test
    fun `Sleep honors milliseconds`() {
        val start = TimeSource.Monotonic.markNow()
        val slept = Thread.sleep(20L)
        val elapsed = start.elapsedNow()
        assertTrue(elapsed >= 20.milliseconds, "Sleep returned too early: $elapsed")
        assertTrue(slept >= 20L, "Incorrect sleep duration: $slept ms")
        assertTrue(slept <= elapsed.inWholeMilliseconds)
    }

    @Test
    fun `Sleep honors a duration`() {
        val start = TimeSource.Monotonic.markNow()
        val slept = Thread.sleep(20.milliseconds)
        val elapsed = start.elapsedNow()
        assertTrue(elapsed >= 20.milliseconds, "Sleep returned too early: $elapsed")
        assertTrue(slept >= 20.milliseconds, "Incorrect sleep duration: $slept")
        assertTrue(slept <= elapsed)
    }

    @Test
    fun `Sleep preserves milliseconds beyond a whole second`() {
        val start = TimeSource.Monotonic.markNow()
        val slept = Thread.sleep(1020L)
        val elapsed = start.elapsedNow()
        assertTrue(elapsed >= 1020.milliseconds, "Sleep returned too early: $elapsed")
        assertTrue(slept >= 1020L, "Incorrect sleep duration: $slept ms")
        assertTrue(slept <= elapsed.inWholeMilliseconds)
    }

    @Test
    fun `Sleep accepts zero`() {
        assertTrue(Thread.sleep(0L) >= 0L)
        assertTrue(Thread.sleep(0.milliseconds) >= 0.milliseconds)
    }

    @Test
    fun `Thread result remains available after joining`() {
        val calls = AtomicInt(0)
        val thread = Thread<Int> {
            calls.fetchAndIncrement()
            42
        }
        thread.join()
        repeat(3) { assertEquals(42, thread.awaitResult()) }
        assertEquals(1, calls.load())
        assertFalse(thread.isAlive)
        assertFalse(thread.isDetached)
        assertEquals(Thread.NO_AFFINITY, thread.affinity)
    }
}