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
import platform.windows.CreateMutexW
import platform.windows.FALSE
import platform.windows.INFINITE
import platform.windows.ReleaseMutex
import platform.windows.WAIT_OBJECT_0
import platform.windows.WaitForSingleObject

private class WindowsMutex(
    val handle: BoxedMutexHandle
) : Mutex {
    override fun lock() {
        val handle = handle.value
        check(handle is WindowsMutexHandle) { "Handle is not a WindowsMutexHandle" }
        WaitForSingleObject(handle.value, INFINITE)
    }

    override fun tryLock(): Boolean {
        val handle = handle.value
        check(handle is WindowsMutexHandle) { "Handle is not a WindowsMutexHandle" }
        return WaitForSingleObject(handle.value, 0U) == WAIT_OBJECT_0
    }

    override fun unlock() {
        val handle = handle.value
        check(handle is WindowsMutexHandle) { "Handle is not a WindowsMutexHandle" }
        ReleaseMutex(handle.value)
    }
}

actual fun Mutex(): Mutex {
    val handle = checkNotNull(CreateMutexW(null, FALSE, null)) {
        "Could not create mutex"
    }
    return WindowsMutex(BoxedMutexHandle(WindowsMutexHandle(handle)))
}