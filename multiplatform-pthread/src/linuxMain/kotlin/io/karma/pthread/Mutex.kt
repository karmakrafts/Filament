package io.karma.pthread

import kotlinx.cinterop.*
import platform.posix.*

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