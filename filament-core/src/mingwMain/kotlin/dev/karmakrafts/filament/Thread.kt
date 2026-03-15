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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import platform.windows.CloseHandle
import platform.windows.CreateThread
import platform.windows.DWORD
import platform.windows.DWORDVar
import platform.windows.GetCurrentThread
import platform.windows.GetCurrentThreadId
import platform.windows.GetLastError
import platform.windows.GetMaximumProcessorCount
import platform.windows.HANDLE
import platform.windows.INFINITE
import platform.windows.LPVOID
import platform.windows.SetThreadGroupAffinity
import platform.windows.Sleep
import platform.windows.WaitForSingleObject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private var threadName: String? = null

@ThreadLocal
private var threadAffinity: Int = Thread.NO_AFFINITY

internal actual fun currentThread(): Thread = WindowsThread(checkNotNull(GetCurrentThread()) {
    "Could not get current thread handle"
}, threadAffinity)

private class WindowsThread( // @formatter:off
    val handle: HANDLE,
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
        WaitForSingleObject(handle, INFINITE)
        CloseHandle(handle) // TODO: check this
    }

    override fun detach() {
        if (_isDetached.compareAndExchange(expectedValue = false, newValue = true)) return
        CloseHandle(handle)
    }
}

private fun threadTrampoline(userData: LPVOID?): DWORD {
    userData?.asStableRef<() -> Unit>()?.apply {
        get()()
        dispose()
    }
    return 0U
}

@PublishedApi
internal actual fun setThreadName(name: String?) {
    threadName = name
}

@PublishedApi
internal actual fun getThreadName(): String = threadName ?: "Thread ${GetCurrentThreadId()}"

@PublishedApi
internal actual fun getThreadId(): ULong = GetCurrentThreadId().toULong()

internal actual fun sleepThread(millis: Long): Long {
    Sleep(millis.toUInt())
    return millis // TODO: actually measure passed time
}

actual fun Thread( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> Unit
): Thread = memScoped { // @formatter:on
    val actualStackSize = if (stackSize == Thread.DEFAULT_STACK_SIZE) 0 else stackSize
    val trampolineAddress = staticCFunction(::threadTrampoline)
    val functionAddress = StableRef.create(function).asCPointer()
    val threadId = alloc<DWORDVar>()
    val handle = checkNotNull(
        CreateThread(
            null, actualStackSize.convert(), trampolineAddress, functionAddress, 0U, threadId.ptr
        )
    ) {
        "Could not create thread"
    }
    // TODO: this code is kind of naive as it assumes all NUMA groups have the same size, but this should be fine for now
    if (affinity != Thread.NO_AFFINITY) {
        val coresPerGroup = GetMaximumProcessorCount(0U.toUShort()).toInt()
        check(affinity < coresPerGroup) { "Affinity must be less than thread group size" }
        val groupIndex = affinity / coresPerGroup
        val coreIndexInGroup = affinity % coresPerGroup
        // Then update to the new custom configuration
        check(SetThreadGroupAffinity(handle, cValue {
            Group = groupIndex.toUShort()
            Mask = Mask or (1UL shl coreIndexInGroup)
        }, null) != 0) { "Could not set thread affinity: 0x${GetLastError().toHexString()}" }
        threadAffinity = affinity
    }
    val thread = WindowsThread(handle, affinity)
    if (detached) thread.detach()
    return thread
}