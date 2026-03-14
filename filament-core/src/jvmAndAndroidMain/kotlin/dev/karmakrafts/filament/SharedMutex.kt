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

@file:JvmName("SharedMutexImpl")

package dev.karmakrafts.filament

import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock

private class JvmSharedMutex : SharedMutex {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private val readLock: ReadLock = lock.readLock()
    private val writeLock: WriteLock = lock.writeLock()

    override val writeLockable: Lockable
        get() = WriteLockable(this)

    override fun lock() = readLock.lock()
    override fun lockWrite() = writeLock.lock()
    override fun tryLockWrite(): Boolean = writeLock.tryLock()
    override fun unlockWrite() = writeLock.unlock()
    override fun tryLock(): Boolean = readLock.tryLock()
    override fun unlock() = readLock.unlock()
}

actual fun SharedMutex(): SharedMutex = JvmSharedMutex()