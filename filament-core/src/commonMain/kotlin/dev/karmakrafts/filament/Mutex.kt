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

@file:JvmName("Mutex$")

package dev.karmakrafts.filament

import kotlin.jvm.JvmName

internal interface MutexHandle

internal expect fun createMutex(): MutexHandle

internal expect fun lockMutex(handle: MutexHandle)

internal expect fun tryLockMutex(handle: MutexHandle): Boolean

internal expect fun unlockMutex(handle: MutexHandle)

/**
 * A mutual exclusion primitive useful for protecting shared state.
 *
 * A Mutex provides a mechanism to ensure that only one thread can access a critical
 * section of code at a time, preventing race conditions in concurrent programming.
 *
 * This interface extends [Lockable], providing the core locking operations:
 * - [lock] to acquire the mutex (blocking until available)
 * - [tryLock] to attempt to acquire the mutex without blocking
 * - [unlock] to release the mutex
 *
 * Example usage:
 * ```
 * val mutex = Mutex()
 * mutex.guarded {
 *     // Critical section - only one thread can execute this at a time
 *     // Access or modify shared resources safely here
 * }
 * ```
 */
interface Mutex : Lockable

private class MutexImpl(
    private val handle: MutexHandle = createMutex()
) : Mutex {
    override fun lock() = lockMutex(handle)

    override fun tryLock(): Boolean = tryLockMutex(handle)

    override fun unlock() = unlockMutex(handle)
}

/**
 * Creates a new [Mutex] instance.
 *
 * This factory function instantiates a platform-specific implementation of the [Mutex] interface.
 * The created mutex is initially unlocked and ready to be used for thread synchronization.
 *
 * Example:
 * ```
 * val mutex = Mutex()
 *
 * // Use with explicit lock/unlock
 * mutex.lock()
 * try {
 *     // Critical section
 * } finally {
 *     mutex.unlock()
 * }
 *
 * // Or use with the guarded extension function
 * mutex.guarded {
 *     // Critical section
 * }
 * ```
 *
 * @return A new [Mutex] instance
 */
fun Mutex(): Mutex = MutexImpl()
