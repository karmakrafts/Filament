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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_tVar
import platform.posix.pthread_mutex_trylock
import platform.posix.pthread_mutex_unlock

@ExperimentalForeignApi
internal actual fun createMutex(): MutexHandle = memScoped {
    val buffer = alloc<pthread_mutex_tVar>()
    pthread_mutex_init(buffer.ptr, null)
    NativeMutexHandle(buffer.value)
}

@ExperimentalForeignApi
internal actual fun destroyMutex(handle: pthread_mutex_t): Unit = memScoped {
    pthread_mutex_destroy(alloc<pthread_mutex_tVar> {
        value = handle
    }.ptr)
}

@ExperimentalForeignApi
internal actual fun lockMutex(handle: MutexHandle): Unit = memScoped {
    require(handle is NativeMutexHandle)
    pthread_mutex_lock(alloc<pthread_mutex_tVar> {
        value = handle.value
    }.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockMutex(handle: MutexHandle): Boolean = memScoped {
    require(handle is NativeMutexHandle)
    pthread_mutex_trylock(alloc<pthread_mutex_tVar> {
        value = handle.value
    }.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockMutex(handle: MutexHandle): Unit = memScoped {
    require(handle is NativeMutexHandle)
    pthread_mutex_unlock(alloc<pthread_mutex_tVar> {
        value = handle.value
    }.ptr)
}