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
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.linux.SYS_gettid
import platform.posix.__NCPUBITS
import platform.posix.__cpu_mask
import platform.posix.cpu_set_t
import platform.posix.nanosleep
import platform.posix.pthread_create
import platform.posix.pthread_detach
import platform.posix.pthread_join
import platform.posix.pthread_self
import platform.posix.pthread_t
import platform.posix.pthread_tVar
import platform.posix.syscall
import platform.posix.timespec
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.concurrent.ThreadLocal

private val activeThreads: ConcurrentMutableSet<pthread_t> = ConcurrentMutableSet()
private val detachedThreads: ConcurrentMutableSet<pthread_t> = ConcurrentMutableSet()

@PublishedApi
internal actual val threadSupportsAffinity: Boolean = true

@ThreadLocal
internal var threadName: String? = null

@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
private fun threadEntryPoint(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get()()
        dispose()
    }
    activeThreads -= pthread_self()
    return null
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun currentThread(): ThreadHandle {
    return NativeThreadHandle(pthread_self())
}

@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
    val handle = alloc<pthread_tVar>()
    val closureAddress = StableRef.create(function).asCPointer()
    pthread_create(handle.ptr, null, staticCFunction(::threadEntryPoint), closureAddress)
    NativeThreadHandle(requireNotNull(handle.value) { "Could not create thread" }).apply {
        activeThreads += value
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
internal actual fun suspendThread(millis: Long): Long = memScoped {
    val spec = alloc<timespec> {
        tv_sec = millis / 1000
        tv_nsec = millis % 1000000
    }
    nanosleep(spec.ptr, spec.ptr)
    (spec.tv_sec * 1000) + (spec.tv_nsec / 1000000)
}

internal actual fun isThreadAlive(handle: ThreadHandle): Boolean {
    require(handle is NativeThreadHandle)
    return handle.value in activeThreads
}

internal actual fun isThreadDetached(handle: ThreadHandle): Boolean {
    require(handle is NativeThreadHandle)
    return handle.value in detachedThreads
}

// Ported from the __CPUELT macro @see https://github.com/Alexpux/Cygwin/blob/master/winsup/cygwin/include/sys/cpuset.h#L21
private fun cpuElt(cpu: Int): Int = cpu / __NCPUBITS.toInt()

// Ported from the __CPUMASK macro @see https://github.com/Alexpux/Cygwin/blob/master/winsup/cygwin/include/sys/cpuset.h#L22
@OptIn(ExperimentalForeignApi::class)
private fun cpuMask(cpu: Int): __cpu_mask = 1.convert<__cpu_mask>() shl (cpu % __NCPUBITS.toInt())

// Ported from the CPU_SET macro @see https://github.com/Alexpux/Cygwin/blob/master/winsup/cygwin/include/sys/cpuset.h#L41
@OptIn(ExperimentalForeignApi::class)
private fun cpuSet(cpu: Int, set: CPointer<cpu_set_t>) {
    if (cpu >= 8 * sizeOf<cpu_set_t>()) return
    set.pointed.apply {
        val index = cpuElt(cpu)
        __bits[index] = __bits[index] or cpuMask(cpu)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun setThreadAffinity(vararg logicalCores: Int) {
    //pthread_setaffinity_np(pthread_self(), sizeOf<cpu_set_t>().convert(), cValue {
    //    for (core in logicalCores) {
    //        cpuSet(core, ptr)
    //    }
    //})
}