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

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.linux.SYS_gettid
import platform.posix.nanosleep
import platform.posix.pthread_self
import platform.posix.pthread_t
import platform.posix.syscall
import platform.posix.timespec
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal var threadName: String? = null

@ThreadLocal
internal var threadAffinity: Int = Thread.NO_AFFINITY

internal actual fun currentThread(): Thread = LinuxThread(pthread_self(), threadAffinity)

private class LinuxThread( // @formatter:off
    val handle: pthread_t,
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

@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
private fun threadTrampoline(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get()()
        dispose()
    }
    return null
}

//@OptIn(ExperimentalForeignApi::class)
//internal actual fun currentThread(): ThreadHandle {
//    return NativeThreadHandle(pthread_self())
//}
//
//@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
//internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
//    val handle = alloc<pthread_tVar>()
//    val closureAddress = StableRef.create(function).asCPointer()
//    pthread_create(handle.ptr, null, staticCFunction(::threadEntryPoint), closureAddress)
//    NativeThreadHandle(requireNotNull(handle.value) { "Could not create thread" }).apply {
//        activeThreads += value
//    }
//}
//
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun joinThread(handle: ThreadHandle) {
//    require(handle is NativeThreadHandle)
//    pthread_join(handle.value, null)
//}
//
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun detachThread(handle: ThreadHandle) {
//    require(handle is NativeThreadHandle)
//    pthread_detach(handle.value)
//    detachedThreads += handle.value
//}

@PublishedApi
@OptIn(ExperimentalForeignApi::class)
internal actual fun setThreadName(name: String?) {
    threadName = name
}

@PublishedApi
@OptIn(ExperimentalForeignApi::class)
internal actual fun getThreadName(): String {
    return threadName ?: "Thread ${getThreadId()}"
}

@PublishedApi
@OptIn(ExperimentalForeignApi::class)
internal actual fun getThreadId(): ULong {
    return syscall(SYS_gettid.convert()).toULong()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun sleepThread(millis: Long): Long = memScoped {
    val spec = alloc<timespec> {
        tv_sec = millis / 1000
        tv_nsec = millis % 1000000
    }
    nanosleep(spec.ptr, spec.ptr)
    (spec.tv_sec * 1000) + (spec.tv_nsec / 1000000)
}

actual fun Thread( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> Unit
): Thread { // @formatter:on
    TODO()
}

//internal actual fun isThreadAlive(handle: ThreadHandle): Boolean {
//    require(handle is NativeThreadHandle)
//    return handle.value in activeThreads
//}
//
//internal actual fun isThreadDetached(handle: ThreadHandle): Boolean {
//    require(handle is NativeThreadHandle)
//    return handle.value in detachedThreads
//}
//
//@OptIn(ExperimentalForeignApi::class)
//internal actual fun setThreadAffinity(logicalCore: Int) {
//    if (logicalCore == Thread.NO_AFFINITY) {
//        val coreCount = sysconf(_SC_NPROCESSORS_ONLN).toInt()
//        LibPthread.pthread_setaffinity_np(pthread_self(), sizeOf<cpu_set_t>().convert(), cValue {
//            for (coreIndex in 0..<coreCount) {
//                LibPthread.CPU_SET(coreIndex, ptr)
//            }
//        })
//        threadAffinity = logicalCore
//        return
//    }
//    LibPthread.pthread_setaffinity_np(pthread_self(), sizeOf<cpu_set_t>().convert(), cValue {
//        LibPthread.CPU_SET(logicalCore, ptr)
//    })
//    threadAffinity = logicalCore
//}
//
//internal actual fun getThreadAffinity(): Int = threadAffinity