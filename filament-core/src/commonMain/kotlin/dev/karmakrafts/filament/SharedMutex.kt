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

/**
 * A reader-writer lock that allows multiple concurrent readers but exclusive writers.
 *
 * SharedMutex extends [Lockable] to provide two types of locks:
 * - Read locks (via [Lockable] methods): Multiple threads can hold read locks simultaneously
 * - Write locks (via write-specific methods): Only one thread can hold a write lock, and no read locks can be held simultaneously
 *
 * This is useful for scenarios where reads are more frequent than writes, allowing for better concurrency
 * than a standard mutex when multiple readers are active.
 */
interface SharedMutex : Lockable {
    /**
     * A [Lockable] that provides access to the write lock functionality.
     *
     * This property allows using the write lock with APIs that accept a [Lockable].
     */
    val writeLockable: Lockable

    /**
     * Acquires the write lock, blocking the current thread until the lock is available.
     *
     * If any read locks or a write lock is already held, this method will block until
     * all locks are released and this thread can acquire the write lock.
     */
    fun lockWrite()

    /**
     * Attempts to acquire the write lock without blocking.
     *
     * @return `true` if the write lock was acquired, `false` otherwise
     */
    fun tryLockWrite(): Boolean

    /**
     * Releases the write lock, allowing other threads to acquire read or write locks.
     *
     * This method should only be called by the thread that currently holds the write lock.
     * Behavior is undefined if called by a thread that does not hold the write lock.
     */
    fun unlockWrite()
}

/**
 * Executes the given [closure] while holding the write lock, ensuring the lock is released afterward.
 *
 * This extension function provides a convenient way to execute code within a critical section
 * protected by this [SharedMutex]'s write lock. It automatically acquires the write lock before
 * executing the closure and guarantees that the lock will be released even if an exception occurs
 * during execution.
 *
 * Unlike [Lockable.guarded] which uses the read lock, this function uses the write lock,
 * providing exclusive access with no concurrent readers or writers.
 *
 * Example usage:
 * ```
 * val sharedMutex = SharedMutex()
 * val result = sharedMutex.guardedWrite {
 *     // Critical section - exclusive access, no other readers or writers
 *     // Modify shared resources safely here
 *     computeResult()
 * }
 * ```
 *
 * @param R The return type of the closure
 * @param closure The code block to execute while holding the write lock
 * @return The result of the [closure] execution
 */
inline fun <reified R> SharedMutex.guardedWrite(closure: () -> R): R {
    try {
        lockWrite()
        return closure()
    }
    finally {
        unlockWrite()
    }
}

/**
 * Attempts to execute the given [closure] while holding the write lock, returning a [defaultValue] if the lock cannot be acquired.
 *
 * Unlike [guardedWrite], this function does not block if the write lock is already held by another thread
 * or if there are active readers. Instead, it makes a single attempt to acquire the write lock using [SharedMutex.tryLockWrite]:
 * - If successful, it executes the [closure] and returns its result
 * - If unsuccessful, it immediately returns the provided [defaultValue] without executing the closure
 *
 * This is useful for non-blocking operations where you want to perform a write operation only if
 * the write lock is immediately available.
 *
 * Example usage:
 * ```
 * val sharedMutex = SharedMutex()
 * val result = sharedMutex.tryGuardedWrite(defaultResult) {
 *     // This code only executes if the write lock was successfully acquired
 *     // Otherwise, defaultResult is returned without executing this block
 *     modifySharedData()
 *     computeResult()
 * }
 * ```
 *
 * @param R The return type of the closure and the default value
 * @param defaultValue The value to return if the write lock cannot be acquired
 * @param closure The code block to execute if the write lock is successfully acquired
 * @return The result of the [closure] execution if the write lock was acquired, or [defaultValue] otherwise
 */
inline fun <reified R> SharedMutex.tryGuardedWrite(defaultValue: R, closure: () -> R): R {
    if (!tryLockWrite()) return defaultValue
    try {
        return closure()
    }
    finally {
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

/**
 * Creates a new [SharedMutex] instance.
 *
 * This factory function instantiates a platform-specific implementation of the [SharedMutex] interface.
 * The created shared mutex is initially unlocked and ready to be used for thread synchronization.
 *
 * A SharedMutex allows multiple concurrent readers but exclusive writers, making it more efficient
 * than a standard mutex in read-heavy scenarios.
 *
 * @return A new [SharedMutex] instance
 */
fun SharedMutex(): SharedMutex = SharedMutexImpl()
