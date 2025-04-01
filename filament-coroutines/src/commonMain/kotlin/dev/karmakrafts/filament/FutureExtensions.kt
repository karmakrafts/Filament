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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

// TODO: document this
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

// TODO: document this
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

// TODO: document this
suspend fun <T> Future<T>.awaitSuspend(): T {
    while(!isCompleted) yield()
    return requireNotNull(value) { "Could not await result of Future" }
}