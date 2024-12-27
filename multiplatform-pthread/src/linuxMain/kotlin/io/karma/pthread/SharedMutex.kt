package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_init
import platform.posix.pthread_rwlock_rdlock
import platform.posix.pthread_rwlock_t
import platform.posix.pthread_rwlock_tryrdlock
import platform.posix.pthread_rwlock_trywrlock
import platform.posix.pthread_rwlock_unlock
import platform.posix.pthread_rwlock_wrlock

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */

@ExperimentalForeignApi
internal actual fun createSharedMutex(): SharedMutexHandle = SharedMutexHandle(nativeHeap.alloc<pthread_rwlock_t> {
    pthread_rwlock_init(ptr, null)
})

@ExperimentalForeignApi
internal actual fun lockSharedMutex(handle: SharedMutexHandle) {
    pthread_rwlock_rdlock(handle.value.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockSharedMutex(handle: SharedMutexHandle): Boolean {
    return pthread_rwlock_tryrdlock(handle.value.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockSharedMutex(handle: SharedMutexHandle) {
    pthread_rwlock_unlock(handle.value.ptr)
}

@ExperimentalForeignApi
internal actual fun lockWriteSharedMutex(handle: SharedMutexHandle) {
    pthread_rwlock_wrlock(handle.value.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockWriteSharedMutex(handle: SharedMutexHandle): Boolean {
    return pthread_rwlock_trywrlock(handle.value.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockWriteSharedMutex(handle: SharedMutexHandle) {
    pthread_rwlock_unlock(handle.value.ptr)
}

@ExperimentalForeignApi
internal actual fun destroySharedMutex(handle: SharedMutexHandle) {
    pthread_rwlock_destroy(handle.value.ptr)
    nativeHeap.free(handle.value)
}