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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalAtomicApi::class)
class ExecutorDispatcherTest {
    companion object {
        private const val THREAD_COUNT: Int = 4
        private const val JOB_COUNT: Int = 1000
        private val TIMEOUT = 10.seconds
    }

    @Test
    fun singleThreadedDispatch() {
        ThreadPool().use { pool ->
            assertDispatch(pool, 1)
        }
    }

    @Test
    fun multiThreadedDispatch() {
        ThreadPool(parallelism = THREAD_COUNT).use { pool ->
            assertDispatch(pool, THREAD_COUNT)
        }
    }

    @Test
    fun dispatchWithDelayedWorkers() {
        val releaseWorkers = AtomicBoolean(false)
        val workerTimedOut = AtomicBoolean(false)
        ThreadPool(threadFactory = { block, index ->
            Thread {
                if (index != 0 && !awaitRelease(releaseWorkers)) {
                    workerTimedOut.store(true)
                }
                block()
            }
        }, parallelism = THREAD_COUNT).use { pool ->
            try {
                assertDispatch(pool, 1)
            }
            finally {
                releaseWorkers.store(true)
            }
        }
        assertFalse(workerTimedOut.load(), "Delayed workers were released by the timeout")
    }

    @Test
    fun allWorkersCanDispatchConcurrently() {
        val count = AtomicInt(0)
        val releaseWorkers = AtomicBoolean(false)
        val allWorkersStarted = CompletableDeferred<Unit>()
        val threadIds = HashSet<ULong>()
        val threadIdsMutex = Mutex()
        ThreadPool(parallelism = THREAD_COUNT).use { pool ->
            val dispatcher = pool.asDispatcher()
            runBlocking {
                withTimeout(TIMEOUT) {
                    try {
                        repeat(THREAD_COUNT) {
                            launch(dispatcher) {
                                val threadId = Thread.id
                                if (count.incrementAndFetch() == THREAD_COUNT) allWorkersStarted.complete(Unit)
                                // Block, rather than suspend, so each job occupies a distinct worker.
                                assertTrue(awaitRelease(releaseWorkers), "Timed out waiting for all workers to start")
                                threadIdsMutex.withLock { threadIds += threadId }
                            }
                        }
                        allWorkersStarted.await()
                    }
                    finally {
                        releaseWorkers.store(true)
                    }
                }
            }
            assertEquals(THREAD_COUNT, threadIds.size)
        }
    }

    @Test
    fun resumeAndYieldOnTheExecutor() {
        val callerId = Thread.id
        ThreadPool().use { pool ->
            val dispatcher = pool.asDispatcher()
            runBlocking {
                withTimeout(TIMEOUT) {
                    val started = CompletableDeferred<Unit>()
                    val resume = CompletableDeferred<Unit>()
                    val threadIds = async(dispatcher) {
                        val before = Thread.id
                        started.complete(Unit)
                        resume.await()
                        val after = Thread.id
                        yield()
                        listOf(before, after, Thread.id)
                    }
                    started.await()
                    // The single worker cannot run this until the coroutine above suspends.
                    withContext(dispatcher) { resume.complete(Unit) }
                    val ids = threadIds.await()
                    assertNotEquals(callerId, ids.first())
                    assertEquals(1, ids.toSet().size)
                }
            }
        }
    }

    @Test
    fun cancelSuspendedCoroutine() {
        val cleanedUp = AtomicBoolean(false)
        ThreadPool().use { pool ->
            val dispatcher = pool.asDispatcher()
            runBlocking {
                withTimeout(TIMEOUT) {
                    val started = CompletableDeferred<Unit>()
                    val job = launch(dispatcher) {
                        try {
                            started.complete(Unit)
                            awaitCancellation()
                        }
                        finally {
                            cleanedUp.store(true)
                        }
                    }
                    started.await()
                    withContext(dispatcher) { job.cancel() }
                    job.join()
                    assertTrue(job.isCancelled)
                    assertTrue(cleanedUp.load())
                    assertEquals(42, async(dispatcher) { 42 }.await())
                }
            }
        }
    }

    @Test
    fun childFailureReachesCallerWithoutStoppingExecutor() {
        val failure = IllegalStateException("Dispatched failure")
        ThreadPool().use { pool ->
            val dispatcher = pool.asDispatcher()
            val actual = assertFailsWith<IllegalStateException> {
                runBlocking {
                    withTimeout(TIMEOUT) {
                        launch(dispatcher) { throw failure }
                    }
                }
            }
            // JVM stacktrace recovery may copy the exception, keeping the original as its cause.
            assertEquals(failure.message, actual.message)
            assertSame(failure, actual.cause ?: actual)
            runBlocking {
                withTimeout(TIMEOUT) {
                    assertEquals(42, async(dispatcher) { 42 }.await())
                }
            }
        }
    }

    private fun assertDispatch(pool: ThreadPool, parallelism: Int) {
        val callerId = Thread.id
        val executions = Array(JOB_COUNT) { AtomicInt(0) }
        val threadIds = HashSet<ULong>()
        val threadIdsMutex = Mutex()
        val dispatcher = pool.asDispatcher()
        runBlocking {
            withTimeout(TIMEOUT) {
                repeat(JOB_COUNT) { index ->
                    launch(dispatcher) {
                        val threadId = Thread.id
                        assertNotEquals(callerId, threadId)
                        executions[index].incrementAndFetch()
                        threadIdsMutex.withLock { threadIds += threadId }
                    }
                }
            }
        }
        executions.forEachIndexed { index, count ->
            assertEquals(1, count.load(), "Job $index should execute exactly once")
        }
        // A shared work queue does not guarantee that every worker receives a short job.
        assertTrue(threadIds.size in 1..parallelism, "Unexpected worker count: ${threadIds.size}")
    }

    private fun awaitRelease(release: AtomicBoolean): Boolean {
        val start = TimeSource.Monotonic.markNow()
        while (!release.load() && start.elapsedNow() < TIMEOUT) Thread.yield()
        return release.load()
    }
}