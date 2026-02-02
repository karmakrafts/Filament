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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalAtomicApi::class)
class ExecutorDispatcherTest {
    companion object {
        private const val THREAD_COUNT: Int = 4
        private const val JOB_COUNT: Int = 1000
    }

    @Test
    fun `Single-threaded dispatch`() {
        val count = AtomicInt(0)
        ThreadPool().use { pool ->
            val scope = CoroutineScope(pool.asDispatcher())
            assertEquals(0, count.load())
            runBlocking {
                (0..<JOB_COUNT).map {
                    scope.launch { count.incrementAndFetch() }
                }.joinAll()
            }
            assertEquals(JOB_COUNT, count.load())
            scope.cancel()
        }
    }

    @Test
    fun `Multi-threaded dispatch`() {
        val count = AtomicInt(0)
        val threadIds = HashMap<ULong, Int>()
        val threadIdsMutex = Mutex()
        ThreadPool(parallelism = THREAD_COUNT).use { pool ->
            val scope = CoroutineScope(pool.asDispatcher())
            assertEquals(0, count.load())
            runBlocking {
                (0..<JOB_COUNT).map {
                    scope.launch {
                        val threadId = Thread.id
                        threadIdsMutex.withLock {
                            threadIds[threadId] = threadIds.getOrPut(threadId) { 0 } + 1
                        }
                        count.incrementAndFetch()
                    }
                }.joinAll()
            }
            assertEquals(JOB_COUNT, count.load())
            assertEquals(THREAD_COUNT, threadIds.size)

            for ((id, count) in threadIds) println("Dispatched $count tasks to thread $id")

            scope.cancel()
        }
    }
}