package io.karma.pthread

import kotlinx.cinterop.*
import platform.posix.*

@ExperimentalForeignApi
private fun threadEntryPoint(userData: COpaquePointer?): COpaquePointer? {
    userData?.asStableRef<() -> Unit>()?.apply {
        get().invoke()
        dispose()
    }
    return null
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun currentThread(): ThreadHandle {
    return ThreadHandle(pthread_self())
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
    val handle = alloc<pthread_tVar>()
    pthread_create(handle.ptr, null, staticCFunction(::threadEntryPoint), StableRef.create(function).asCPointer())
    ThreadHandle(requireNotNull(handle.value) { "Could not create thread" })
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun joinThread(handle: ThreadHandle) {
    pthread_join(handle.value, null)
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun detachThread(handle: ThreadHandle) {
    pthread_detach(handle.value)
}