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

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun createMutex(): MutexHandle = NativeMutexHandle(nativeHeap.alloc<pthread_mutex_t> {
    pthread_mutex_init(ptr, null)
})

@OptIn(ExperimentalForeignApi::class)
internal actual fun destroyMutex(handle: pthread_mutex_t) {
    pthread_mutex_destroy(handle.ptr)
    nativeHeap.free(handle)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun lockMutex(handle: MutexHandle) {
    require(handle is NativeMutexHandle)
    pthread_mutex_lock(handle.value.ptr)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun tryLockMutex(handle: MutexHandle): Boolean {
    require(handle is NativeMutexHandle)
    return pthread_mutex_trylock(handle.value.ptr) == 1
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun unlockMutex(handle: MutexHandle) {
    require(handle is NativeMutexHandle)
    pthread_mutex_unlock(handle.value.ptr)
}