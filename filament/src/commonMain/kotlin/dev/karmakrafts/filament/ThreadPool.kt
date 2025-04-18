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

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

// TODO: document this
typealias ThreadFactory = (block: () -> Unit, index: Int) -> Thread

// TODO: document this
val defaultThreadFactory: ThreadFactory = { block, _ -> Thread(block) }

// TODO: document this
@OptIn(ExperimentalAtomicApi::class)
class ThreadPool( // @formatter:off
    threadFactory: ThreadFactory = defaultThreadFactory,
    parallelism: Int = 1
) : Executor, AutoCloseable { // @formatter:on
    @PublishedApi
    internal val _isRunning: AtomicBoolean = AtomicBoolean(true)

    private val taskCount: AtomicLong = AtomicLong(0)

    @PublishedApi
    internal val tasks: ArrayDeque<() -> Unit> = ArrayDeque()

    @PublishedApi
    internal val tasksMutex: SharedMutex = SharedMutex()

    inline val activeJobs: List<() -> Unit>
        get() = tasksMutex.guarded { tasks.toCollection(ArrayList()) }

    inline val isRunning: Boolean
        get() = _isRunning.load()

    override fun enqueueTask(task: () -> Unit) {
        tasksMutex.guardedWrite {
            tasks += task
        }
        taskCount.incrementAndFetch()
    }

    private val threads: Array<Thread> = Array(parallelism) { index ->
        threadFactory(::threadMain, index)
    }

    private fun threadMain() {
        while (_isRunning.load()) {
            while (_isRunning.load() && taskCount.load() == 0L) Thread.yield()
            tasksMutex.guardedWrite(tasks::removeFirstOrNull)?.let { block ->
                block()
                taskCount.decrementAndFetch()
            }
        }
    }

    override fun close() {
        if (!_isRunning.compareAndSet(expectedValue = true, newValue = false)) return
        for (thread in threads) thread.join()
    }
}