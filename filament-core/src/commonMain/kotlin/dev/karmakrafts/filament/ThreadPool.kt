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

/**
 * A function type that creates threads for a thread pool.
 *
 * @param block The function to be executed by the thread.
 * @param index The index of the thread in the pool, useful for naming or configuring threads.
 * @return A new Thread instance.
 */
typealias ThreadFactory = (block: () -> Unit, index: Int) -> Thread

/**
 * The default thread factory implementation that creates basic threads.
 * This factory ignores the index parameter and simply creates a thread with the given block.
 */
val defaultThreadFactory: ThreadFactory = { block, _ -> Thread(block) }

/**
 * A pool of worker threads that can execute tasks concurrently.
 * ThreadPool implements the Executor interface for task submission and AutoCloseable for resource cleanup.
 *
 * @property threadFactory The factory used to create worker threads. Defaults to [defaultThreadFactory].
 * @property parallelism The number of worker threads in the pool. Defaults to 1.
 */
@OptIn(ExperimentalAtomicApi::class)
class ThreadPool( // @formatter:off
    threadFactory: ThreadFactory = defaultThreadFactory,
    parallelism: Int = 1
) : Executor { // @formatter:on
    @PublishedApi
    internal val _isRunning: AtomicBoolean = AtomicBoolean(true)

    private val taskCount: AtomicLong = AtomicLong(0)

    @PublishedApi
    internal val tasks: ArrayDeque<() -> Unit> = ArrayDeque()

    @PublishedApi
    internal val tasksMutex: SharedMutex = SharedMutex()

    /**
     * Gets a list of all tasks currently in the queue.
     * This property is thread-safe and returns a snapshot of the current tasks.
     *
     * @return A list containing all tasks currently in the queue.
     */
    inline val activeJobs: List<() -> Unit>
        get() = tasksMutex.guarded { tasks.toCollection(ArrayList()) }

    /**
     * Indicates whether the thread pool is currently running.
     * When this property is false, the pool is shutting down or has been shut down.
     *
     * @return True if the thread pool is running, false otherwise.
     */
    inline val isRunning: Boolean
        get() = _isRunning.load()

    /**
     * Adds a task to the thread pool's queue for execution.
     * The task will be executed by one of the worker threads when available.
     *
     * @param task The function to be executed.
     */
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

    /**
     * Shuts down the thread pool and waits for all worker threads to complete.
     * This method is idempotent - calling it multiple times has no additional effect.
     * After closing, no new tasks will be processed, but any tasks already in progress will complete.
     */
    override fun close() {
        if (!_isRunning.compareAndSet(expectedValue = true, newValue = false)) return
        for (thread in threads) thread.join()
    }
}
