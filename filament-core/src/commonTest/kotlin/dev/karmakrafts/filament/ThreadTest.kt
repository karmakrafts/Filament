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
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ThreadTest {
    @Test
    fun `Create and join`() {
        val thread = Thread {}
        thread.join()
    }

    @Test
    fun `Check current thread ID`() {
        val id = Thread.id
        println("Current thread ID: $id")
        assertNotEquals(0UL, id)
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

    @OptIn(ExperimentalAtomicApi::class)
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
            } while (!release.load() && start.elapsedNow() < 5.seconds)
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
        Thread {
            val id = Thread.id
            println("Separate thread ID: $id")
            assertNotEquals(0UL, id)
            assertNotEquals(parentId, id)
        }.join()
    }

    @Test
    fun `Set and get thread affinity`() {
        Thread(1) { // Thread with affinity 1
            val coreCount = Processor.logicalCores
            assertTrue(coreCount > 0)
        }.join()
    }

    @Test
    fun `Thread with result`() {
        val result = Thread<String> {
            "Hello, World!"
        }.awaitResult()
        assertEquals("Hello, World!", result)
    }
}