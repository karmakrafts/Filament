/*
 * Copyright 2025 Karma Krafts
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
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A future implementation that can be completed manually.
 * This class allows setting a value that can be awaited by other threads.
 *
 * @param T The type of the value that the future will complete with.
 * @param defaultValue The initial value of the future, defaults to null.
 */
@OptIn(ExperimentalAtomicApi::class)
class CompletableFuture<T>( // @formatter:off
    defaultValue: T? = null
) : Future<T> { // @formatter:on
    companion object {
        /**
         * Creates a CompletableFuture that will be completed with the result of the provided block.
         * The block will be executed asynchronously on the provided executor.
         *
         * @param T The type of the value that the future will complete with.
         * @param executor The executor to run the block on.
         * @param block The function to execute asynchronously.
         * @return A CompletableFuture that will be completed with the result of the block.
         */
        inline fun <T> async(executor: Executor, crossinline block: () -> T): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            executor.enqueueTask { future.complete(block()) }
            return future
        }
    }

    private var _value: AtomicReference<T?> = AtomicReference(defaultValue)
    private var _isCompleted: AtomicBoolean = AtomicBoolean(false)
    private var _completionCallback: AtomicReference<((T) -> Unit)?> = AtomicReference(null)

    override val value: T?
        get() = _value.load()

    override val isCompleted: Boolean
        get() = _isCompleted.load()

    /**
     * Completes this future with the given value.
     * If the future is already completed, this method does nothing.
     * Upon completion, any registered completion callbacks will be invoked.
     *
     * @param value The value to complete the future with.
     */
    fun complete(value: T) {
        if (!_isCompleted.compareAndSet(expectedValue = false, newValue = true)) return
        this._value.store(value)
        _completionCallback.load()?.invoke(value)
    }

    /**
     * Registers a callback to be invoked when this future completes.
     * If the future is already completed, the callback will be invoked immediately.
     * Multiple callbacks can be registered; they will be invoked in the order they were registered.
     *
     * @param callback The function to invoke when the future completes.
     * @return This future, for method chaining.
     */
    fun onCompletion(callback: CompletionCallback<T>): Future<T> {
        val currentCallback = _completionCallback.load()
        if (currentCallback == null) {
            _completionCallback.store(callback)
            return this
        }
        _completionCallback.store { value ->
            currentCallback(value)
            callback(value)
        }
        return this
    }
}

/**
 * A barrier is a specialized CompletableFuture that completes with Unit.
 * It can be used for synchronization between threads, where the completion
 * of the barrier signals that some operation has completed.
 */
typealias Barrier = CompletableFuture<Unit>
