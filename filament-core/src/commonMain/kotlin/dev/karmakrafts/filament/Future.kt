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

/**
 * A callback function that is invoked when a future completes.
 *
 * @param T The type of the value that the future will complete with.
 */
typealias CompletionCallback<T> = (T) -> Unit

/**
 * Represents a value that may be available in the future.
 * This interface provides methods to check if the value is available and to wait for it.
 *
 * @param T The type of the value that the future will complete with.
 */
interface Future<T> {
    /**
     * The current value of the future, or null if the future has not completed yet.
     */
    val value: T?

    /**
     * Indicates whether the future has completed.
     * When this is true, [value] will contain the result.
     */
    val isCompleted: Boolean

    /**
     * Blocks the current thread until the future completes and returns the result.
     * This method will yield the current thread's execution time to other threads
     * while waiting for the result.
     *
     * @return The value that the future completed with.
     * @throws IllegalStateException If the future completed but the value is null.
     */
    fun await(): T {
        while (!isCompleted) Thread.yield()
        return requireNotNull(value) { "Could not await result of CompletableFuture" }
    }
}
