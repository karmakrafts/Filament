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
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal var threadName: String? = null

@ExperimentalForeignApi
private fun threadEntryPoint(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get().invoke()
        dispose()
    }
    return null
}

@OptIn(UnsafeNumber::class)
internal actual fun currentThread(): ThreadHandle {
    return NativeThreadHandle(pthread_self())
}

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
    val handle = alloc<pthread_tVar>()
    pthread_create(handle.ptr, null, staticCFunction(::threadEntryPoint), StableRef.create(function).asCPointer())
    NativeThreadHandle(requireNotNull(handle.value) { "Could not create thread" })
}

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun joinThread(handle: ThreadHandle) {
    require(handle is NativeThreadHandle)
    pthread_join(handle.value, null)
}

@OptIn(UnsafeNumber::class)
internal actual fun detachThread(handle: ThreadHandle) {
    require(handle is NativeThreadHandle)
    pthread_detach(handle.value)
}

@OptIn(UnsafeNumber::class)
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(pthread_self(), name)
    threadName = name
}

internal actual fun getThreadName(): String {
    return threadName ?: "Thread ${getThreadId()}"
}

@OptIn(UnsafeNumber::class)
internal actual fun getThreadId(): ULong {
    return pthread_gettid_np(pthread_self()).toULong()
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