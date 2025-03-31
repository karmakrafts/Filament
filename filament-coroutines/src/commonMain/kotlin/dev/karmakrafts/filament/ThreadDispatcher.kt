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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

// TODO: document this
typealias DispatcherThreadFactory = (block: () -> Unit, index: Int) -> Thread

// TODO: document this
val defaultDispatcherThreadFactory: DispatcherThreadFactory = { block, _ -> Thread(block) }

// TODO: document this
@OptIn(ExperimentalAtomicApi::class)
open class ThreadDispatcher( // @formatter:off
    threadFactory: DispatcherThreadFactory = defaultDispatcherThreadFactory,
    parallelism: Int = 1
) : CoroutineDispatcher(), AutoCloseable { // @formatter:on
    private val jobs: ArrayList<Runnable> = ArrayList()
    private val jobsMutex: Mutex = Mutex()
    private val isRunning: AtomicBoolean = AtomicBoolean(true)

    private val threads: Array<Thread> = Array(parallelism) { index ->
        threadFactory(::threadMain, index)
    }

    private fun threadMain() {
        while (isRunning.load()) {
            jobsMutex.guarded {
                if (jobs.isEmpty()) return@guarded
                jobs.removeFirst().run()
            }
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        jobsMutex.guarded {
            jobs += block
        }
    }

    override fun close() {
        if (!isRunning.compareAndSet(expectedValue = true, newValue = false)) return
        for (thread in threads) thread.join()
    }
}