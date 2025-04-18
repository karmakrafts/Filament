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

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
private fun threadEntryPoint(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get().invoke()
        dispose()
    }
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
    NativeThreadHandle(requireNotNull(handle.value) { "Could not create thread" })
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
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(name)
}

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun getThreadName(): String = memScoped {
    val nameBuffer = allocArray<ByteVar>(4096).reinterpret<ByteVar>().pointed
    pthread_getname_np(pthread_self(), nameBuffer.ptr, 4096U)
    return nameBuffer.ptr.toKString().ifBlank { "Thread ${getThreadId()}" }
}

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