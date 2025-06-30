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

import java.util.concurrent.locks.ReentrantReadWriteLock

internal data class JvmSharedMutexHandle(
    val value: ReentrantReadWriteLock
) : SharedMutexHandle

internal actual fun createSharedMutex(): SharedMutexHandle = JvmSharedMutexHandle(ReentrantReadWriteLock())

internal actual fun lockSharedMutex(handle: SharedMutexHandle) {
    require(handle is JvmSharedMutexHandle)
    handle.value.readLock().lock()
}

internal actual fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean {
    require(handle is JvmSharedMutexHandle)
    return handle.value.readLock().tryLock()
}

internal actual fun unlockSharedMutex(handle: SharedMutexHandle) {
    require(handle is JvmSharedMutexHandle)
    handle.value.readLock().unlock()
}

internal actual fun lockWriteSharedMutex(handle: SharedMutexHandle) {
    require(handle is JvmSharedMutexHandle)
    handle.value.writeLock().lock()
}

internal actual fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean {
    require(handle is JvmSharedMutexHandle)
    return handle.value.writeLock().tryLock()
}

internal actual fun unlockWriteSharedMutex(handle: SharedMutexHandle) {
    require(handle is JvmSharedMutexHandle)
    handle.value.writeLock().unlock()
}