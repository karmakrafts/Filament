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
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import platform.windows.CloseHandle
import platform.windows.HANDLE
import platform.windows.SRWLOCK

internal value class WindowsMutexHandle(
    val value: HANDLE
) : MutexHandle {
    override fun close() {
        CloseHandle(value)
    }
}

internal value class WindowsSharedMutexHandle(
    val value: SRWLOCK
) : SharedMutexHandle {
    override fun close() {
        nativeHeap.free(value)
    }
}