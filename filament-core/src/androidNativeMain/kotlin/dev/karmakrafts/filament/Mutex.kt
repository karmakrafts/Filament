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
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_trylock
import platform.posix.pthread_mutex_unlock

private class AndroidMutex(
    val handle: NativeMutexHandle
) : Mutex {
    override fun lock() {
        pthread_mutex_lock(handle.value.ptr)
    }

    override fun tryLock(): Boolean {
        return pthread_mutex_trylock(handle.value.ptr) == 1
    }

    override fun unlock() {
        pthread_mutex_unlock(handle.value.ptr)
    }
}

actual fun Mutex(): Mutex = AndroidMutex(NativeMutexHandle(nativeHeap.alloc<pthread_mutex_t> {
    pthread_mutex_init(ptr, null)
}))

internal actual fun destroyMutex(handle: pthread_mutex_t) {
    pthread_mutex_destroy(handle.ptr)
    nativeHeap.free(handle)
}