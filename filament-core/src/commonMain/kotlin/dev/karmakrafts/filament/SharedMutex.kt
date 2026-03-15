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

package dev.karmakrafts.filament

/**
 * An implementation of [Lockable] that delegates to the write lock of a [SharedMutex].
 *
 * This class allows using the write lock of a [SharedMutex] in contexts where a [Lockable]
 * is required, such as in the [withLock] extension function.
 *
 * @property mutex The [SharedMutex] whose write lock this [Lockable] delegates to.
 */
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
 * - **Read locks** (via [Lockable] methods): Multiple threads can hold read locks simultaneously,
 *   as long as no write lock is held.
 * - **Write locks** (via [lockWrite], [tryLockWrite], and [unlockWrite]): Only one thread can hold
 *   a write lock at any given time. No read locks can be held while a write lock is active.
 *
 * This primitive is useful for scenarios where data is read frequently but modified
 * infrequently, as it allows for better concurrency than a standard [Mutex] when multiple
 * readers are active.
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
 * Unlike [withLock] which uses the read lock, this function uses the write lock,
 * providing exclusive access with no concurrent readers or writers.
 *
 * Example usage:
 * ```kotlin
 * val sharedMutex = SharedMutex()
 * val result = sharedMutex.withWriteLock {
 *     // Critical section - exclusive access, no other readers or writers
 *     // Modify shared resources safely here
 *     computeResult()
 * }
 * ```
 *
 * @param R The return type of the closure.
 * @param closure The code block to execute while holding the write lock.
 * @return The result of the [closure] execution.
 */
inline fun <reified R> SharedMutex.withWriteLock(closure: () -> R): R {
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
 * Unlike [withWriteLock], this function does not block if the write lock is already held by another thread
 * or if there are active readers. Instead, it makes a single attempt to acquire the write lock using [SharedMutex.tryLockWrite]:
 * - If successful, it executes the [closure] and returns its result
 * - If unsuccessful, it immediately returns the provided [defaultValue] without executing the closure
 *
 * This is useful for non-blocking operations where you want to perform a write operation only if
 * the write lock is immediately available.
 *
 * Example usage:
 * ```kotlin
 * val sharedMutex = SharedMutex()
 * val result = sharedMutex.tryWithWriteLock(defaultResult) {
 *     // This code only executes if the write lock was successfully acquired
 *     // Otherwise, defaultResult is returned without executing this block
 *     modifySharedData()
 *     computeResult()
 * }
 * ```
 *
 * @param R The return type of the closure and the default value.
 * @param defaultValue The value to return if the write lock cannot be acquired.
 * @param closure The code block to execute if the write lock is successfully acquired.
 * @return The result of the [closure] execution if the write lock was acquired, or [defaultValue] otherwise.
 */
inline fun <reified R> SharedMutex.tryWithWriteLock(defaultValue: R, closure: () -> R): R {
    if (!tryLockWrite()) return defaultValue
    try {
        return closure()
    }
    finally {
        unlockWrite()
    }
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
expect fun SharedMutex(): SharedMutex
