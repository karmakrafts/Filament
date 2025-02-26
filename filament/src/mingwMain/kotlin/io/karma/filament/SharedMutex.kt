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

package io.karma.filament

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_init
import platform.posix.pthread_rwlock_rdlock
import platform.posix.pthread_rwlock_t
import platform.posix.pthread_rwlock_tVar
import platform.posix.pthread_rwlock_tryrdlock
import platform.posix.pthread_rwlock_trywrlock
import platform.posix.pthread_rwlock_unlock
import platform.posix.pthread_rwlock_wrlock

@ExperimentalForeignApi
internal actual fun createSharedMutex(): SharedMutexHandle = memScoped {
    val handle = alloc<pthread_rwlock_tVar>()
    pthread_rwlock_init(handle.ptr, null)
    NativeSharedMutexHandle(handle.value)
}

@ExperimentalForeignApi
internal actual fun lockSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_rdlock(alloc<pthread_rwlock_tVar> {
        value = handle.value
    }.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean = memScoped {
    require(handle is NativeSharedMutexHandle)
    return pthread_rwlock_tryrdlock(alloc<pthread_rwlock_tVar> {
        value = handle.value
    }.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_unlock(alloc<pthread_rwlock_tVar> {
        value = handle.value
    }.ptr)
}

@ExperimentalForeignApi
internal actual fun lockWriteSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_wrlock(alloc<pthread_rwlock_tVar> {
        value = handle.value
    }.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean = memScoped {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_trywrlock(alloc<pthread_rwlock_tVar> {
        value = handle.value
    }.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockWriteSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    require(handle is NativeSharedMutexHandle)
    pthread_rwlock_unlock(alloc<pthread_rwlock_tVar> {
        value = handle.value
    }.ptr)
}

@ExperimentalForeignApi
internal actual fun destroySharedMutex(handle: pthread_rwlock_t): Unit = memScoped {
    pthread_rwlock_destroy(alloc<pthread_rwlock_tVar> {
        value = handle
    }.ptr)
}