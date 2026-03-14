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

@file:OptIn(ExperimentalForeignApi::class)

package dev.karmakrafts.filament

import co.touchlab.stately.collections.ConcurrentMutableSet
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import platform.posix.nanosleep
import platform.posix.pthread_getname_np
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_t
import platform.posix.timespec
import platform.windows.GetCurrentThread
import platform.windows.HANDLE
import kotlin.native.concurrent.ThreadLocal

private val detachedThreads: ConcurrentMutableSet<pthread_t> = ConcurrentMutableSet()

@ThreadLocal
private var previousThreadGroup: UShort = 0U

@ThreadLocal
private var previousThreadAffinity: ULong = 0UL

@ThreadLocal
private var threadAffinity: Int = Thread.NO_AFFINITY

internal actual fun currentThread(): Thread = WindowsThread(checkNotNull(GetCurrentThread()) {
    "Could not get current thread handle"
}, threadAffinity)

private class WindowsThread( // @formatter:off
    val handle: HANDLE,
    override val affinity: Int
) : Thread { // @formatter:on
    override val isAlive: Boolean
        get() = TODO("Not yet implemented")
    override val isDetached: Boolean
        get() = TODO("Not yet implemented")

    override fun join() {
        TODO("Not yet implemented")
    }

    override fun detach() {
        TODO("Not yet implemented")
    }
}

private fun threadTrampoline(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get()()
        dispose()
    }
    return null
}

//@ExperimentalForeignApi
//internal actual fun currentThread(): ThreadHandle {
//    return NativeThreadHandle(pthread_self())
//}
//
//@ExperimentalForeignApi
//internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
//    val handle = alloc<pthread_tVar>()
//    pthread_create(handle.ptr, null, staticCFunction(::threadEntryPoint), StableRef.create(function).asCPointer())
//    NativeThreadHandle(requireNotNull(handle.value) { "Could not create thread" })
//}
//
//@ExperimentalForeignApi
//internal actual fun joinThread(handle: ThreadHandle) {
//    require(handle is NativeThreadHandle)
//    pthread_join(handle.value, null)
//}
//
//@ExperimentalForeignApi
//internal actual fun detachThread(handle: ThreadHandle) {
//    require(handle is NativeThreadHandle)
//    pthread_detach(handle.value)
//    detachedThreads += handle.value
//}

@PublishedApi
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(pthread_self(), name)
}

@PublishedApi
@ExperimentalForeignApi
internal actual fun getThreadName(): String = memScoped {
    val nameBuffer = allocArray<ByteVar>(4096).reinterpret<ByteVar>().pointed
    pthread_getname_np(pthread_self(), nameBuffer.ptr, 4096U)
    nameBuffer.ptr.toKString().ifBlank { "Thread ${pthread_self()}" }
}

@PublishedApi
@ExperimentalForeignApi
internal actual fun getThreadId(): ULong {
    return pthread_self()
}

@ExperimentalForeignApi
internal actual fun sleepThread(millis: Long): Long = memScoped {
    val spec = alloc<timespec> {
        tv_sec = millis / 1000
        tv_nsec = (millis % 1000000).toInt()
    }
    nanosleep(spec.ptr, spec.ptr)
    (spec.tv_sec * 1000) + (spec.tv_nsec / 1000000)
}

actual fun Thread( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> Unit
): Thread = TODO() // @formatter:on

//@ExperimentalForeignApi
//internal actual fun isThreadAlive(handle: ThreadHandle): Boolean = memScoped {
//    require(handle is NativeThreadHandle)
//    val result = alloc<COpaquePointerVar>()
//    _pthread_tryjoin(handle.value, result.ptr) != 0
//}
//
//internal actual fun isThreadDetached(handle: ThreadHandle): Boolean {
//    require(handle is NativeThreadHandle)
//    return handle.value in detachedThreads
//}
//
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun setThreadAffinity(logicalCore: Int) = memScoped {
//    val handle = GetCurrentThread() ?: return@memScoped
//
//    // If no affinity is specified, we reset the affinity state
//    if (logicalCore == Thread.NO_AFFINITY) {
//        check(SetThreadGroupAffinity(handle, cValue {
//            Group = previousThreadGroup
//            Mask = previousThreadAffinity
//        }, null) != 0) { "Could not restore thread affinity" }
//        previousThreadGroup = 0U
//        previousThreadAffinity = 0UL
//        return@memScoped
//    }
//
//    // Calculate the thread group of the first specified core
//    val groupCount = GetMaximumProcessorGroupCount()
//    val coresPerGroup = GetMaximumProcessorCount(groupCount).toInt()
//    val groupIndex = logicalCore / coresPerGroup
//
//    // Save the default affinity group and mask if it's not already saved
//    if (previousThreadGroup == 0U.toUShort() && previousThreadAffinity == 0UL) {
//        val affinity = alloc<GROUP_AFFINITY>()
//        GetThreadGroupAffinity(handle, affinity.ptr)
//        previousThreadGroup = affinity.Group
//        previousThreadAffinity = affinity.Mask
//    }
//
//    // Then update to the new custom configuration
//    check(SetThreadGroupAffinity(handle, cValue {
//        Group = groupIndex.toUShort()
//        val coreGroupIndex = logicalCore / coresPerGroup
//        check(coreGroupIndex == groupIndex) {
//            "Logical core $logicalCore does not belong to thread group $groupIndex"
//        }
//        val coreIndexInGroup = logicalCore % coresPerGroup
//        Mask = Mask or (1UL shl coreIndexInGroup)
//    }, null) != 0) { "Could not set thread affinity" }
//}
//
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun getThreadAffinity(): Int = memScoped {
//    val handle = GetCurrentThread() ?: return -1
//    val affinity = alloc<GROUP_AFFINITY>()
//    GetThreadGroupAffinity(handle, affinity.ptr)
//    val groupCount = GetMaximumProcessorGroupCount().toInt()
//    var coreOffset = 0
//    for (groupIndex in 0..<groupCount) {
//        if (groupIndex != affinity.Group.toInt()) continue
//        val coreCount = GetMaximumProcessorCount(groupIndex.toUShort()).toInt()
//        for (coreIndex in 0..<coreCount) {
//            if (affinity.Mask and (1UL shl coreIndex) == 0UL) continue
//            return coreOffset + coreIndex
//        }
//        coreOffset += coreCount
//    }
//    return -1
//}