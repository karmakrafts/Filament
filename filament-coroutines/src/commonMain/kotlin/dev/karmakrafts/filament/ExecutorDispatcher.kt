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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] implementation that delegates task execution to an [Executor].
 * This class allows using Filament's [Executor] implementations with Kotlin coroutines.
 *
 * @property executor The [Executor] to which tasks will be delegated.
 */
@OptIn(ExperimentalAtomicApi::class, InternalCoroutinesApi::class)
internal class ExecutorDispatcher(
    private val executor: Executor, parentContext: CoroutineContext?
) : CoroutineDispatcher() {
    init {
        parentContext?.get(Job)?.invokeOnCompletion(onCancelling = true) {
            executor.close() // Shut down underlying executor when parent job completes/cancels
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.enqueueTask(block::run)
    }
}

/**
 * Converts this [Executor] to a [CoroutineDispatcher] that can be used with Kotlin coroutines.
 * This allows using any [Executor] implementation as a coroutine dispatcher.
 *
 * @param parentContext A coroutine context used as the parent context of the newly created Executor.
 * @return A [CoroutineDispatcher] that delegates task execution to this [Executor].
 */
fun Executor.asDispatcher(
    parentContext: CoroutineContext? = null
): CoroutineDispatcher = ExecutorDispatcher(this, parentContext)
