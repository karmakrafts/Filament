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

// TODO: document this
interface ThreadWithResult<R> : Thread {
    // TODO: document this
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

// TODO: document this
fun <R> Thread(
    affinity: Int = Thread.NO_AFFINITY,
    stackSize: Long = Thread.DEFAULT_STACK_SIZE,
    detached: Boolean = false,
    function: () -> R
): ThreadWithResult<R> = ThreadWithResultImpl(affinity, stackSize, detached, function)