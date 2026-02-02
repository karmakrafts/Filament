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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/**
 * Converts a [Deferred] to a [Future].
 * This allows using a coroutine's [Deferred] result with Filament's [Future] API.
 *
 * @param T The type of the value that the future will complete with.
 * @return A [Future] that completes when the [Deferred] completes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Deferred<T>.asFuture(): Future<T> = object : Future<T> {
    override val value: T
        get() = getCompleted()

    override val isCompleted: Boolean
        get() = this@asFuture.isCompleted

    override fun await(): T = runBlocking {
        this@asFuture.await()
    }
}

/**
 * Creates a [CompletableFuture] that will be completed with the result of the provided suspending block.
 * The block will be executed asynchronously on the provided executor.
 * This is similar to [CompletableFuture.async], but supports suspending functions.
 *
 * @param T The type of the value that the future will complete with.
 * @param executor The executor to run the block on.
 * @param block The suspending function to execute asynchronously.
 * @return A [CompletableFuture] that will be completed with the result of the block.
 */
inline fun <T> CompletableFuture.Companion.asyncSuspend( // @formatter:off
    executor: Executor,
    crossinline block: suspend () -> T
): CompletableFuture<T> { // @formatter:on
    val future = CompletableFuture<T>()
    executor.enqueueTask {
        runBlocking { future.complete(block()) }
    }
    return future
}

/**
 * Suspends the current coroutine until the future completes and returns the result.
 * This method will yield the current coroutine's execution time to other coroutines
 * while waiting for the result, rather than blocking the thread like [Future.await].
 *
 * @param T The type of the value that the future will complete with.
 * @return The value that the future completed with.
 * @throws IllegalArgumentException If the future completed but the value is null.
 */
suspend fun <T> Future<T>.awaitSuspend(): T {
    while (!isCompleted) yield()
    return requireNotNull(value) { "Could not await result of Future" }
}
