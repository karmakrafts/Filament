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

@file:JvmName("SharedMutex$")

package dev.karmakrafts.filament

import kotlin.jvm.JvmName

internal interface SharedMutexHandle

internal expect fun createSharedMutex(): SharedMutexHandle

internal expect fun lockSharedMutex(handle: SharedMutexHandle)

internal expect fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean

internal expect fun unlockSharedMutex(handle: SharedMutexHandle)

internal expect fun lockWriteSharedMutex(handle: SharedMutexHandle)

internal expect fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean

internal expect fun unlockWriteSharedMutex(handle: SharedMutexHandle)

@PublishedApi
internal class WriteLockable(
    private val mutex: SharedMutex
) : Lockable {
    override fun lock() = mutex.lockWrite()

    override fun tryLock(): Boolean = mutex.tryLockWrite()

    override fun unlock() = mutex.unlockWrite()
}

interface SharedMutex : Lockable {
    val writeLockable: Lockable
    fun lockWrite()
    fun tryLockWrite(): Boolean
    fun unlockWrite()
}

inline fun <reified R> SharedMutex.guardedWrite(closure: () -> R): R {
    try {
        lockWrite()
        return closure()
    } finally {
        unlockWrite()
    }
}

inline fun <reified R> SharedMutex.tryGuardedWrite(defaultValue: R, closure: () -> R): R {
    if (!tryLockWrite()) return defaultValue
    try {
        return closure()
    } finally {
        unlockWrite()
    }
}

private class SharedMutexImpl(
    private val handle: SharedMutexHandle = createSharedMutex()
) : SharedMutex {
    override val writeLockable: Lockable
        get() = WriteLockable(this)

    override fun lock() = lockSharedMutex(handle)

    override fun tryLock(): Boolean = tryLockSharedMutex(handle)

    override fun unlock() = unlockSharedMutex(handle)

    override fun lockWrite() = lockWriteSharedMutex(handle)

    override fun tryLockWrite(): Boolean = tryLockWriteSharedMutex(handle)

    override fun unlockWrite() = unlockWriteSharedMutex(handle)
}

fun SharedMutex(): SharedMutex = SharedMutexImpl()