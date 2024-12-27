package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_init
import platform.posix.pthread_rwlock_rdlock
import platform.posix.pthread_rwlock_tVar
import platform.posix.pthread_rwlock_tryrdlock
import platform.posix.pthread_rwlock_trywrlock
import platform.posix.pthread_rwlock_unlock
import platform.posix.pthread_rwlock_wrlock

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */

@ExperimentalForeignApi
internal actual fun createSharedMutex(): SharedMutexHandle = memScoped {
    val handle = alloc<pthread_rwlock_tVar>()
    pthread_rwlock_init(handle.ptr, null)
    SharedMutexHandle(handle.value)
}

@ExperimentalForeignApi
internal actual fun lockSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    pthread_rwlock_rdlock(buffer.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    return pthread_rwlock_tryrdlock(buffer.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    pthread_rwlock_unlock(buffer.ptr)
}

@ExperimentalForeignApi
internal actual fun lockWriteSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    pthread_rwlock_wrlock(buffer.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    pthread_rwlock_trywrlock(buffer.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockWriteSharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    pthread_rwlock_unlock(buffer.ptr)
}

@ExperimentalForeignApi
internal actual fun destroySharedMutex(handle: SharedMutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_rwlock_tVar> {
        value = handle.value
    }
    pthread_rwlock_destroy(buffer.ptr)
}