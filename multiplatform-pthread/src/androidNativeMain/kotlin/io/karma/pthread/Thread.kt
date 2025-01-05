package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.pthread_create
import platform.posix.pthread_detach
import platform.posix.pthread_gettid_np
import platform.posix.pthread_join
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_tVar
import kotlin.native.concurrent.ThreadLocal

@PublishedApi
@ThreadLocal
internal var threadName: String? = null

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun currentThread(): ThreadHandle {
    return ThreadHandle(pthread_self())
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun createThread(function: () -> Unit): ThreadHandle = memScoped {
    val handle = alloc<pthread_tVar>()
    pthread_create(handle.ptr, null, staticCFunction { userData ->
        userData?.asStableRef<() -> Unit>()?.apply {
            get().invoke()
            dispose()
        }
        null
    }, StableRef.create(function).asCPointer())
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

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(pthread_self(), name)
    threadName = name
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun getThreadName(): String {
    return threadName ?: "Thread ${pthread_self()}"
}

@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
internal actual fun getThreadId(): ULong {
    return pthread_gettid_np(pthread_self()).toULong()
}