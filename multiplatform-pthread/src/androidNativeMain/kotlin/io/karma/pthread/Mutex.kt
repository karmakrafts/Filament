package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_trylock
import platform.posix.pthread_mutex_unlock

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun createMutex(): MutexHandle = MutexHandle(nativeHeap.alloc<pthread_mutex_t> {
    pthread_mutex_init(ptr, null)
})

@ExperimentalForeignApi
internal actual fun destroyMutex(handle: MutexHandle) {
    pthread_mutex_destroy(handle.value.ptr)
    nativeHeap.free(handle.value)
}

@ExperimentalForeignApi
internal actual fun lockMutex(handle: MutexHandle) {
    pthread_mutex_lock(handle.value.ptr)
}

@ExperimentalForeignApi
internal actual fun tryLockMutex(handle: MutexHandle): Boolean {
    return pthread_mutex_trylock(handle.value.ptr) == 1
}

@ExperimentalForeignApi
internal actual fun unlockMutex(handle: MutexHandle) {
    pthread_mutex_unlock(handle.value.ptr)
}