package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_t

@ExperimentalForeignApi
internal data class ThreadHandle(
    val value: pthread_t
)

@ExperimentalForeignApi
internal expect fun currentThread(): ThreadHandle

@ExperimentalForeignApi
internal expect fun createThread(function: () -> Unit): ThreadHandle

@ExperimentalForeignApi
internal expect fun joinThread(handle: ThreadHandle)

@ExperimentalForeignApi
internal expect fun detachThread(handle: ThreadHandle)

value class Thread @OptIn(ExperimentalForeignApi::class) private constructor(
    private val handle: ThreadHandle
) {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun create(function: () -> Unit): Thread = Thread(createThread(function))

        @OptIn(ExperimentalForeignApi::class)
        fun current(): Thread = Thread(currentThread())
    }

    @OptIn(ExperimentalForeignApi::class)
    fun join() = joinThread(handle)

    @OptIn(ExperimentalForeignApi::class)
    fun detach() = detachThread(handle)
}