/*
 * Copyright 2025 Karma Krafts & associates
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
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalAtomicApi::class)
class ThreadDispatcherTest {
    companion object {
        private const val THREAD_COUNT: Int = 4
        private const val JOB_COUNT: Int = 1000
    }

    @Test
    fun `Single-threaded dispatch`() {
        val count = AtomicInt(0)
        ThreadDispatcher().use { dispatcher ->
            val scope = CoroutineScope(dispatcher)
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
        val threadIds = HashSet<ULong>()
        ThreadDispatcher(parallelism = THREAD_COUNT).use { dispatcher ->
            val scope = CoroutineScope(dispatcher)
            assertEquals(0, count.load())
            runBlocking {
                (0..<JOB_COUNT).map {
                    scope.launch {
                        threadIds += Thread.id
                        count.incrementAndFetch()
                    }
                }.joinAll()
            }
            assertEquals(JOB_COUNT, count.load())
            assertEquals(THREAD_COUNT, threadIds.size)
            scope.cancel()
        }
    }
}