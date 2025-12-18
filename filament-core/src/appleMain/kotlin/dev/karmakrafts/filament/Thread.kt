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

import co.touchlab.stately.collections.ConcurrentMutableSet
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.nanosleep
import platform.posix.pthread_create
import platform.posix.pthread_detach
import platform.posix.pthread_getname_np
import platform.posix.pthread_join
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_t
import platform.posix.pthread_tVar
import platform.posix.pthread_threadid_np
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
private val activeThreads: ConcurrentMutableSet<pthread_t> = ConcurrentMutableSet()

@OptIn(ExperimentalForeignApi::class)
private val detachedThreads: ConcurrentMutableSet<pthread_t> = ConcurrentMutableSet()

@PublishedApi
internal actual val threadSupportsAffinity: Boolean = false

@OptIn(ExperimentalForeignApi::class)
private fun threadEntryPoint(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get().invoke()
        dispose()
    }
    activeThreads.remove(pthread_self())
    return null
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun currentThread(): ThreadHandle {
    return NativeThreadHandle(requireNotNull(pthread_self()) { "Could not retrieve current thread" })
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
    val handle = alloc<pthread_tVar>()
    pthread_create(handle.ptr, null, staticCFunction(::threadEntryPoint), StableRef.create(function).asCPointer())
    NativeThreadHandle(requireNotNull(handle.value) { "Could not create thread" }).apply {
        activeThreads.add(value)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun joinThread(handle: ThreadHandle) {
    require(handle is NativeThreadHandle)
    pthread_join(handle.value, null)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun detachThread(handle: ThreadHandle) {
    require(handle is NativeThreadHandle)
    pthread_detach(handle.value)
    detachedThreads += handle.value
}

@PublishedApi
@OptIn(ExperimentalForeignApi::class)
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(name)
}

@PublishedApi
@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun getThreadName(): String = memScoped {
    val nameBuffer = allocArray<ByteVar>(4096).reinterpret<ByteVar>().pointed
    pthread_getname_np(pthread_self(), nameBuffer.ptr, 4096U)
    return nameBuffer.ptr.toKString().ifBlank { "Thread ${getThreadId()}" }
}

@PublishedApi
@OptIn(ExperimentalForeignApi::class)
internal actual fun getThreadId(): ULong = memScoped {
    val id = alloc<ULongVar>()
    pthread_threadid_np(pthread_self(), id.ptr)
    id.value
}

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun suspendThread(millis: Long): Long = memScoped {
    val spec = alloc<timespec> {
        tv_sec = (millis / 1000).convert()
        tv_nsec = (millis % 1000000).convert()
    }
    nanosleep(spec.ptr, spec.ptr)
    (spec.tv_sec * 1000).toLong() + (spec.tv_nsec / 1000000)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun isThreadAlive(handle: ThreadHandle): Boolean {
    require(handle is NativeThreadHandle)
    return handle.value in activeThreads
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun isThreadDetached(handle: ThreadHandle): Boolean {
    require(handle is NativeThreadHandle)
    return handle.value in detachedThreads
}

internal actual fun setThreadAffinity(vararg logicalCores: Int) = Unit