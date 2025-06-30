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

@file:JvmName("Lockable$")

package dev.karmakrafts.filament

import kotlin.jvm.JvmName

/**
 * A base interface for synchronization primitives that provide locking mechanisms.
 *
 * This interface defines the core operations needed for thread synchronization:
 * - [lock] to acquire exclusive access (blocking until available)
 * - [tryLock] to attempt to acquire exclusive access without blocking
 * - [unlock] to release exclusive access
 *
 * Implementations of this interface can be used to protect critical sections of code
 * from concurrent access by multiple threads, preventing race conditions.
 *
 * @see Mutex for a concrete implementation
 */
interface Lockable {
    /**
     * Acquires the lock, blocking the current thread until the lock is available.
     *
     * If the lock is already held by another thread, this method will block until
     * the lock becomes available.
     */
    fun lock()

    /**
     * Attempts to acquire the lock without blocking.
     *
     * @return `true` if the lock was acquired, `false` otherwise
     */
    fun tryLock(): Boolean

    /**
     * Releases the lock, allowing other threads to acquire it.
     *
     * This method should only be called by the thread that currently holds the lock.
     * Behavior is undefined if called by a thread that does not hold the lock.
     */
    fun unlock()
}

/**
 * Executes the given [closure] while holding the lock, ensuring the lock is released afterward.
 *
 * This extension function provides a convenient way to execute code within a critical section
 * protected by this [Lockable]. It automatically acquires the lock before executing the closure
 * and guarantees that the lock will be released even if an exception occurs during execution.
 *
 * Example usage:
 * ```
 * val lockable = Mutex()
 * val result = lockable.guarded {
 *     // Critical section - only one thread can execute this at a time
 *     // Access or modify shared resources safely here
 *     computeResult()
 * }
 * ```
 *
 * @param R The return type of the closure
 * @param closure The code block to execute while holding the lock
 * @return The result of the [closure] execution
 */
inline fun <reified R> Lockable.guarded(closure: () -> R): R {
    return try {
        lock()
        closure()
    }
    finally {
        unlock()
    }
}

/**
 * Attempts to execute the given [closure] while holding the lock, returning a [defaultValue] if the lock cannot be acquired.
 *
 * Unlike [guarded], this function does not block if the lock is already held by another thread.
 * Instead, it makes a single attempt to acquire the lock using [Lockable.tryLock]:
 * - If successful, it executes the [closure] and returns its result
 * - If unsuccessful, it immediately returns the provided [defaultValue] without executing the closure
 *
 * This is useful for non-blocking operations where you want to perform an action only if
 * the lock is immediately available.
 *
 * Example usage:
 * ```
 * val lockable = Mutex()
 * val result = lockable.tryGuarded(defaultResult) {
 *     // This code only executes if the lock was successfully acquired
 *     // Otherwise, defaultResult is returned without executing this block
 *     computeResult()
 * }
 * ```
 *
 * @param R The return type of the closure and the default value
 * @param defaultValue The value to return if the lock cannot be acquired
 * @param closure The code block to execute if the lock is successfully acquired
 * @return The result of the [closure] execution if the lock was acquired, or [defaultValue] otherwise
 */
inline fun <reified R> Lockable.tryGuarded(defaultValue: R, closure: () -> R): R {
    if (!tryLock()) return defaultValue
    return try {
        closure()
    }
    finally {
        unlock()
    }
}
