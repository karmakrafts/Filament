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
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_init
import platform.posix.pthread_rwlock_rdlock
import platform.posix.pthread_rwlock_t
import platform.posix.pthread_rwlock_tryrdlock
import platform.posix.pthread_rwlock_trywrlock
import platform.posix.pthread_rwlock_unlock
import platform.posix.pthread_rwlock_wrlock

private class AppleSharedMutex(
    val handle: NativeSharedMutexHandle
) : SharedMutex {
    override val writeLockable: Lockable
        get() = WriteLockable(this)

    override fun lockWrite() {
        pthread_rwlock_wrlock(handle.value.ptr)
    }

    override fun tryLockWrite(): Boolean {
        return pthread_rwlock_trywrlock(handle.value.ptr) == 1
    }

    override fun unlockWrite() {
        pthread_rwlock_unlock(handle.value.ptr)
    }

    override fun lock() {
        pthread_rwlock_rdlock(handle.value.ptr)
    }

    override fun tryLock(): Boolean {
        return pthread_rwlock_tryrdlock(handle.value.ptr) == 1
    }

    override fun unlock() {
        pthread_rwlock_unlock(handle.value.ptr)
    }
}

actual fun SharedMutex(): SharedMutex = AppleSharedMutex(NativeSharedMutexHandle(nativeHeap.alloc<pthread_rwlock_t> {
    pthread_rwlock_init(ptr, null)
}))

@OptIn(ExperimentalForeignApi::class)
internal actual fun destroySharedMutex(handle: pthread_rwlock_t) {
    pthread_rwlock_destroy(handle.ptr)
    nativeHeap.free(handle)
}