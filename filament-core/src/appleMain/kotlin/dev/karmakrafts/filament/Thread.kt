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

@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)

package dev.karmakrafts.filament

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
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
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.nanosleep
import platform.posix.pthread_detach
import platform.posix.pthread_getname_np
import platform.posix.pthread_join
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_t
import platform.posix.pthread_threadid_np
import platform.posix.timespec
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal var threadAffinity: Int = Thread.NO_AFFINITY

internal actual fun currentThread(): Thread = AppleThread(checkNotNull(pthread_self()) {
    "Could not get current thread handle"
}, threadAffinity)

internal class AppleThread( // @formatter:off
    val handle: pthread_t,
    override val affinity: Int
) : Thread { // @formatter:on
    private val _isAlive: AtomicBoolean = AtomicBoolean(true)
    override val isAlive: Boolean
        get() = _isAlive.load()

    private val _isDetached: AtomicBoolean = AtomicBoolean(false)
    override val isDetached: Boolean
        get() = _isDetached.load()

    override fun join() {
        if (!_isAlive.compareAndSet(expectedValue = true, newValue = false)) return
        pthread_join(handle, null)
    }

    override fun detach() {
        if (!_isDetached.compareAndSet(expectedValue = false, newValue = true)) return
        pthread_detach(handle)
    }
}

internal fun threadTrampoline(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get().invoke()
        dispose()
    }
    return null
}

@PublishedApi
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(name)
}

@PublishedApi
internal actual fun getThreadName(): String = memScoped {
    val nameBuffer = allocArray<ByteVar>(4096).reinterpret<ByteVar>().pointed
    pthread_getname_np(pthread_self(), nameBuffer.ptr, 4096U)
    return nameBuffer.ptr.toKString().ifBlank { "Thread ${getThreadId()}" }
}

@PublishedApi
internal actual fun getThreadId(): ULong = memScoped {
    val id = alloc<ULongVar>()
    pthread_threadid_np(pthread_self(), id.ptr)
    id.value
}

internal actual fun sleepThread(millis: Long): Long = memScoped {
    val spec = alloc<timespec> {
        tv_sec = (millis / 1000).convert()
        tv_nsec = (millis % 1000000).convert()
    }
    nanosleep(spec.ptr, spec.ptr)
    (spec.tv_sec * 1000).convert<Long>() + (spec.tv_nsec / 1000000)
}

// The Darwin kernel does not support per-core affinity like Linux, so we emulate
// core mappings through thread affinity tags
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun setThreadAffinity(logicalCore: Int) = memScoped {
//    val handle = pthread_mach_thread_np(pthread_self())
//    val policy = alloc<thread_affinity_policy_data_t> {
//        affinity_tag = logicalCore
//    }
//    val result = thread_policy_set( // @formatter:off
//        thread = handle,
//        flavor = THREAD_AFFINITY_POLICY.toUInt(),
//        policy_info = policy.ptr.reinterpret(),
//        policy_infoCnt = THREAD_AFFINITY_POLICY_COUNT
//    ) // @formatter:on
//    check(result == KERN_SUCCESS) { "Could not update thread affinity tag" }
//}
//
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun getThreadAffinity(): Int = memScoped {
//    val handle = pthread_mach_thread_np(pthread_self())
//    val policy = alloc<thread_affinity_policy_data_t>()
//    thread_policy_get(
//        thread = handle,
//        flavor = THREAD_AFFINITY_POLICY.toUInt(),
//        policy_info = policy.ptr.reinterpret(),
//        policy_infoCnt = null,
//        get_default = null
//    )
//    return policy.affinity_tag
//}