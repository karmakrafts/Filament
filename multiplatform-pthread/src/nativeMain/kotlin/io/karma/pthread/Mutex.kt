package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_mutex_t

@ExperimentalForeignApi
internal data class MutexHandle(
    val value: pthread_mutex_t
)

@ExperimentalForeignApi
internal expect fun createMutex(): MutexHandle

@ExperimentalForeignApi
internal expect fun destroyMutex(handle: MutexHandle)

@ExperimentalForeignApi
internal expect fun lockMutex(handle: MutexHandle)

@ExperimentalForeignApi
internal expect fun tryLockMutex(handle: MutexHandle): Boolean

@ExperimentalForeignApi
internal expect fun unlockMutex(handle: MutexHandle)

value class Mutex @OptIn(ExperimentalForeignApi::class) private constructor(
    private val handle: MutexHandle
) : Lockable, AutoCloseable {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun create(): Mutex = Mutex(createMutex())
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun close() = destroyMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    override fun lock() = lockMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    override fun tryLock(): Boolean = tryLockMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    override fun unlock() = unlockMutex(handle)
}