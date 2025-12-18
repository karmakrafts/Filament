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
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_init
import platform.posix.pthread_rwlock_rdlock
import platform.posix.pthread_rwlock_t
import platform.posix.pthread_rwlock_tryrdlock
import platform.posix.pthread_rwlock_trywrlock
import platform.posix.pthread_rwlock_unlock
import platform.posix.pthread_rwlock_wrlock

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun createSharedMutex(): SharedMutexHandle =
    NativeSharedMutexHandle(nativeHeap.alloc<pthread_rwlock_t> {
        pthread_rwlock_init(ptr, null)
    })

@OptIn(ExperimentalForeignApi::class)
internal actual fun lockSharedMutex(handle: SharedMutexHandle) {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_rdlock(handle.value.ptr)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean {
    require(handle is NativeSharedMutexHandle)
    return pthread_rwlock_tryrdlock(handle.value.ptr) == 1
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun unlockSharedMutex(handle: SharedMutexHandle) {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_unlock(handle.value.ptr)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun lockWriteSharedMutex(handle: SharedMutexHandle) {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_wrlock(handle.value.ptr)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean {
    require(handle is NativeSharedMutexHandle)
    return pthread_rwlock_trywrlock(handle.value.ptr) == 1
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun unlockWriteSharedMutex(handle: SharedMutexHandle) {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_unlock(handle.value.ptr)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun destroySharedMutex(handle: pthread_rwlock_t) {
    pthread_rwlock_destroy(handle.ptr)
    nativeHeap.free(handle)
}