package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_rwlock_t

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */

@ExperimentalForeignApi
internal data class SharedMutexHandle(
    val value: pthread_rwlock_t
)

@ExperimentalForeignApi
internal expect fun createSharedMutex(): SharedMutexHandle

@ExperimentalForeignApi
internal expect fun lockSharedMutex(handle: SharedMutexHandle)

@ExperimentalForeignApi
internal expect fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean

@ExperimentalForeignApi
internal expect fun unlockSharedMutex(handle: SharedMutexHandle)

@ExperimentalForeignApi
internal expect fun lockWriteSharedMutex(handle: SharedMutexHandle)

@ExperimentalForeignApi
internal expect fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean

@ExperimentalForeignApi
internal expect fun unlockWriteSharedMutex(handle: SharedMutexHandle)

@ExperimentalForeignApi
internal expect fun destroySharedMutex(handle: SharedMutexHandle)

value class SharedMutex @OptIn(ExperimentalForeignApi::class) private constructor(
    private val handle: SharedMutexHandle
) : Lockable, AutoCloseable {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun create(): SharedMutex = SharedMutex(createSharedMutex())
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun close() = destroySharedMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    override fun lock() = lockSharedMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    override fun tryLock(): Boolean = tryLockSharedMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    override fun unlock() = unlockSharedMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    fun lockWrite() = lockWriteSharedMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    fun tryLockWrite(): Boolean = tryLockWriteSharedMutex(handle)

    @OptIn(ExperimentalForeignApi::class)
    fun unlockWrite() = unlockWriteSharedMutex(handle)

    inline fun <reified R> guardedWrite(closure: () -> R): R {
        try {
            lockWrite()
            return closure()
        }
        finally {
            unlockWrite()
        }
    }

    inline fun <reified R> tryGuardedWrite(defaultValue: R, closure: () -> R): R {
        try {
            if (!tryLockWrite()) return defaultValue
            return closure()
        }
        finally {
            unlockWrite()
        }
    }
}