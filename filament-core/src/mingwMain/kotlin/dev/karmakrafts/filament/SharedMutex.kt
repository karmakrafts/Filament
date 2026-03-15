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
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.windows.AcquireSRWLockExclusive
import platform.windows.AcquireSRWLockShared
import platform.windows.InitializeSRWLock
import platform.windows.ReleaseSRWLockExclusive
import platform.windows.ReleaseSRWLockShared
import platform.windows.SRWLOCK
import platform.windows.TRUE
import platform.windows.TryAcquireSRWLockExclusive
import platform.windows.TryAcquireSRWLockShared

private class WindowsSharedMutex(
    val handle: BoxedSharedMutexHandle
) : SharedMutex {
    override val writeLockable: Lockable
        get() = WriteLockable(this)

    override fun lockWrite() {
        val handle = handle.value
        check(handle is WindowsSharedMutexHandle) { "Handle is not a WindowsSharedMutexHandle" }
        AcquireSRWLockExclusive(handle.value.ptr)
    }

    override fun tryLockWrite(): Boolean {
        val handle = handle.value
        check(handle is WindowsSharedMutexHandle) { "Handle is not a WindowsSharedMutexHandle" }
        return TryAcquireSRWLockExclusive(handle.value.ptr) == TRUE.toUByte()
    }

    override fun unlockWrite() {
        val handle = handle.value
        check(handle is WindowsSharedMutexHandle) { "Handle is not a WindowsSharedMutexHandle" }
        ReleaseSRWLockExclusive(handle.value.ptr)
    }

    override fun lock() {
        val handle = handle.value
        check(handle is WindowsSharedMutexHandle) { "Handle is not a WindowsSharedMutexHandle" }
        AcquireSRWLockShared(handle.value.ptr)
    }

    override fun tryLock(): Boolean {
        val handle = handle.value
        check(handle is WindowsSharedMutexHandle) { "Handle is not a WindowsSharedMutexHandle" }
        return TryAcquireSRWLockShared(handle.value.ptr) == TRUE.toUByte()
    }

    override fun unlock() {
        val handle = handle.value
        check(handle is WindowsSharedMutexHandle) { "Handle is not a WindowsSharedMutexHandle" }
        ReleaseSRWLockShared(handle.value.ptr)
    }
}

actual fun SharedMutex(): SharedMutex {
    val handle = nativeHeap.alloc<SRWLOCK> {
        InitializeSRWLock(ptr)
    }
    return WindowsSharedMutex(BoxedSharedMutexHandle(WindowsSharedMutexHandle(handle)))
}