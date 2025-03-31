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
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// TODO: document this
@OptIn(ExperimentalAtomicApi::class)
class CompletableFuture<T>( // @formatter:off
    defaultValue: T? = null
) : Future<T> { // @formatter:on
    companion object {
        inline fun <T> async(executor: Executor, crossinline block: () -> T): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            executor.enqueueTask { future.complete(block()) }
            return future
        }
    }

    private var _value: AtomicReference<T?> = AtomicReference(defaultValue)
    private var _isCompleted: AtomicBoolean = AtomicBoolean(false)

    override val value: T?
        get() = _value.load()

    override val isCompleted: Boolean
        get() = _isCompleted.load()

    fun complete(value: T) {
        if (!_isCompleted.compareAndSet(expectedValue = false, newValue = true)) return
        this._value.store(value)
    }

    override fun await(): T {
        while (!_isCompleted.load()) Thread.yield()
        return requireNotNull(_value.load()) { "Could not await result of CompletableFuture" }
    }
}

// TODO: document this
typealias Barrier = CompletableFuture<Unit>