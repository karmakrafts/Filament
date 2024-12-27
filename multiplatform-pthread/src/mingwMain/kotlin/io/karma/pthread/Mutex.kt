package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_tVar
import platform.posix.pthread_mutex_trylock
import platform.posix.pthread_mutex_unlock

@ExperimentalForeignApi
internal actual fun createMutex(): MutexHandle = memScoped {
    val buffer = alloc<pthread_mutex_tVar>()
    pthread_mutex_init(buffer.ptr, null)
    MutexHandle(buffer.value)
}

@ExperimentalForeignApi
internal actual fun destroyMutex(handle: MutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_mutex_tVar> {
        value = handle.value
    }
    pthread_mutex_destroy(buffer.ptr)
}

@ExperimentalForeignApi
internal actual fun lockMutex(handle: MutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_mutex_tVar> {
        value = handle.value
    }
    pthread_mutex_lock(buffer.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockMutex(handle: MutexHandle): Boolean = memScoped {
    val buffer = alloc<pthread_mutex_tVar> {
        value = handle.value
    }
    pthread_mutex_trylock(buffer.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockMutex(handle: MutexHandle): Unit = memScoped {
    val buffer = alloc<pthread_mutex_tVar> {
        value = handle.value
    }
    pthread_mutex_unlock(buffer.ptr)
}