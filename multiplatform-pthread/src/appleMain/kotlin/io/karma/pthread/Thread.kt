package io.karma.pthread

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.pthread_create
import platform.posix.pthread_detach
import platform.posix.pthread_getname_np
import platform.posix.pthread_join
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_tVar
import platform.posix.pthread_threadid_np

@PublishedApi
@ExperimentalForeignApi
internal actual fun currentThread(): ThreadHandle {
    return ThreadHandle(requireNotNull(pthread_self()) { "Could not retrieve current thread" })
}

@PublishedApi
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

@PublishedApi
@ExperimentalForeignApi
internal actual fun joinThread(handle: ThreadHandle) {
    pthread_join(handle.value, null)
}

@PublishedApi
@ExperimentalForeignApi
internal actual fun detachThread(handle: ThreadHandle) {
    pthread_detach(handle.value)
}

@PublishedApi
@ExperimentalForeignApi
internal actual fun setThreadName(name: String?) {
    pthread_setname_np(name)
}

@PublishedApi
@ExperimentalForeignApi
internal actual fun getThreadName(): String = memScoped {
    val nameBuffer = allocArray<ByteVar>(4096).reinterpret<ByteVar>().pointed
    pthread_getname_np(pthread_self(), nameBuffer.ptr, 4096U)
    return nameBuffer.ptr.toKString().ifBlank { "Thread ${getThreadId()}" }
}

@PublishedApi
@ExperimentalForeignApi
internal actual fun getThreadId(): ULong = memScoped {
    val id = alloc<ULongVar>()
    pthread_threadid_np(pthread_self(), id.ptr)
    id.value
}