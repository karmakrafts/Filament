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

@file:Suppress("DEPRECATION_ERROR") // Coroutines..

package dev.karmakrafts.filament

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ChildHandle
import kotlinx.coroutines.ChildJob
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.InternalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext

private object CFDeferredKey : CoroutineContext.Key<Deferred<*>>

@OptIn(InternalCoroutinesApi::class)
private object CFChildHandle : ChildHandle {
    @InternalCoroutinesApi
    override fun childCancelled(cause: Throwable): Boolean = true

    @InternalCoroutinesApi
    override val parent: Job? = null

    override fun dispose() = Unit
}

private object CFDisposableHandle : DisposableHandle {
    override fun dispose() = Unit
}

// TODO: document this
@OptIn(InternalForInheritanceCoroutinesApi::class)
fun <T> Future<T>.asDeferred(): Deferred<T> {
    return object : Deferred<T> {
        private val cancellationException: CancellationException = CancellationException("Deferred was cancelled")

        override suspend fun await(): T {
            while (!isCompleted) yield()
            return value!!
        }

        @ExperimentalCoroutinesApi
        override fun getCompleted(): T {
            if (!isCompleted) throw getCompletionExceptionOrNull()!!
            return value!!
        }

        @ExperimentalCoroutinesApi
        override fun getCompletionExceptionOrNull(): Throwable? {
            return if (isCompleted) null
            else IllegalStateException("Deferred has not completed")
        }

        override val onAwait: SelectClause1<T>
            get() = TODO("Not implemented")

        @InternalCoroutinesApi
        override fun attachChild(child: ChildJob): ChildHandle = CFChildHandle

        override fun cancel(cause: CancellationException?) = Unit

        override fun cancel(cause: Throwable?): Boolean = false

        @InternalCoroutinesApi
        override fun getCancellationException(): CancellationException = cancellationException

        override fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle = CFDisposableHandle

        @InternalCoroutinesApi
        override fun invokeOnCompletion(
            onCancelling: Boolean, invokeImmediately: Boolean, handler: CompletionHandler
        ): DisposableHandle = CFDisposableHandle

        override suspend fun join() {
            while (!isCompleted) yield()
        }

        override fun start(): Boolean = true

        override val children: Sequence<Job> = emptySequence()
        override val isActive: Boolean
            get() = !isCompleted
        override val isCancelled: Boolean = false
        override val isCompleted: Boolean
            get() = isCompleted
        override val onJoin: SelectClause0
            get() = TODO("Not yet implemented")

        @ExperimentalCoroutinesApi
        override val parent: Job? = null
        override val key: CoroutineContext.Key<*> = CFDeferredKey
    }
}