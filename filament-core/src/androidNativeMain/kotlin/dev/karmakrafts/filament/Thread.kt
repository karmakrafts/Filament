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

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.PTHREAD_CREATE_DETACHED
import platform.posix.PTHREAD_CREATE_JOINABLE
import platform.posix.nanosleep
import platform.posix.pthread_attr_init
import platform.posix.pthread_attr_setdetachstate
import platform.posix.pthread_attr_setstacksize
import platform.posix.pthread_attr_t
import platform.posix.pthread_create
import platform.posix.pthread_detach
import platform.posix.pthread_gettid_np
import platform.posix.pthread_join
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_t
import platform.posix.pthread_tVar
import platform.posix.timespec
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal var threadAffinity: Int = Thread.NO_AFFINITY

@ThreadLocal
internal var threadName: String? = null

internal actual fun currentThread(): Thread = AndroidThread(pthread_self(), threadAffinity)

private class AndroidThread( // @formatter:off
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
        if (!_isAlive.compareAndExchange(expectedValue = true, newValue = false)) return
        pthread_join(handle, null)
    }

    override fun detach() {
        if (_isDetached.compareAndExchange(expectedValue = false, newValue = true)) return
        pthread_detach(handle)
    }
}

private fun threadTrampoline(userData: COpaquePointer?): COpaquePointer? {
    val ref = userData?.asStableRef<() -> Unit>() ?: return null
    ref.get()()
    ref.dispose()
    return null
}

@PublishedApi
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(pthread_self(), name)
    threadName = name
}

@PublishedApi
internal actual fun getThreadName(): String {
    return threadName ?: "Thread ${getThreadId()}"
}

@PublishedApi
internal actual fun getThreadId(): ULong {
    return pthread_gettid_np(pthread_self()).toULong()
}

internal actual fun sleepThread(millis: Long): Long = memScoped {
    val spec = alloc<timespec> {
        tv_sec = (millis / 1000).convert()
        tv_nsec = (millis % 1000000).convert()
    }
    nanosleep(spec.ptr, spec.ptr)
    (spec.tv_sec * 1000).toLong() + (spec.tv_nsec / 1000000)
}

actual fun Thread( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> Unit
): Thread = memScoped { // @formatter:on
    val handle = alloc<pthread_tVar>()
    val attributes = alloc<pthread_attr_t>()
    check(pthread_attr_init(attributes.ptr) == 0) {
        "Could not initialize thread attributes"
    }
    if (stackSize != Thread.DEFAULT_STACK_SIZE) {
        check(pthread_attr_setstacksize(attributes.ptr, stackSize.convert()) == 0) {
            "Could not set thread stack size"
        }
    }
    val detachState = if (detached) PTHREAD_CREATE_DETACHED else PTHREAD_CREATE_JOINABLE
    check(pthread_attr_setdetachstate(attributes.ptr, detachState) == 0) {
        "Could not set thread detach state"
    }
    val trampolineAddress = staticCFunction(::threadTrampoline)
    val closureAddress = StableRef.create(function).asCPointer()
    check(pthread_create(handle.ptr, attributes.ptr, trampolineAddress, closureAddress) == 0) {
        "Could not create thread"
    }
    threadAffinity = affinity
    AndroidThread(handle.value, affinity)
}