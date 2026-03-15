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

/**
 * Represents a [Thread] that returns a result after its execution has completed.
 *
 * @param R The type of the result returned by the thread.
 */
interface ThreadWithResult<R> : Thread {
    /**
     * Blocks the current thread until this thread has completed and returns its result.
     *
     * This method internally calls [join] to ensure the thread has finished before
     * retrieving the result.
     *
     * @return The result produced by the thread's execution.
     */
    fun awaitResult(): R
}

private class ThreadWithResultImpl<R>( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> R
) : ThreadWithResult<R> { // @formatter:on
    private val result: CompletableFuture<R> = CompletableFuture()

    private val delegate: Thread = Thread( // @formatter:off
        affinity = affinity,
        stackSize = stackSize,
        detached = detached,
        function = { result.complete(function()) }
    ) // @formatter:on

    override val affinity: Int
        get() = delegate.affinity
    override val isAlive: Boolean
        get() = delegate.isAlive
    override val isDetached: Boolean
        get() = delegate.isDetached

    override fun detach() = delegate.detach()

    override fun awaitResult(): R {
        delegate.join()
        return result.await()
    }

    override fun join() {
        awaitResult()
    }
}

/**
 * Creates and starts a new [ThreadWithResult].
 *
 * @param R The type of the result returned by the thread.
 * @param affinity The CPU affinity for the new thread. Defaults to [Thread.NO_AFFINITY].
 * @param stackSize The stack size for the new thread in bytes. Defaults to [Thread.DEFAULT_STACK_SIZE].
 * @param detached Whether the thread should be created in a detached state. Defaults to `false`.
 * @param function The block of code to be executed by the thread, which returns a result.
 * @return A new [ThreadWithResult] instance.
 */
fun <R> Thread(
    affinity: Int = Thread.NO_AFFINITY,
    stackSize: Long = Thread.DEFAULT_STACK_SIZE,
    detached: Boolean = false,
    function: () -> R
): ThreadWithResult<R> = ThreadWithResultImpl(affinity, stackSize, detached, function)