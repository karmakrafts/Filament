package io.karma.pthread

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_t
import platform.posix.sched_yield

@ExperimentalForeignApi
internal data class ThreadHandle(
    val value: pthread_t
)

@PublishedApi
@ExperimentalForeignApi
internal expect fun currentThread(): ThreadHandle

@PublishedApi
@ExperimentalForeignApi
internal expect fun createThread(function: () -> Unit): ThreadHandle

@PublishedApi
@ExperimentalForeignApi
internal expect fun joinThread(handle: ThreadHandle)

@PublishedApi
@ExperimentalForeignApi
internal expect fun detachThread(handle: ThreadHandle)

@PublishedApi
@ExperimentalForeignApi
internal expect fun setThreadName(name: String?)

@PublishedApi
@ExperimentalForeignApi
internal expect fun getThreadName(): String

@PublishedApi
@ExperimentalForeignApi
internal expect fun getThreadId(): ULong

@Suppress("NOTHING_TO_INLINE")
value class Thread @OptIn(ExperimentalForeignApi::class) @PublishedApi internal constructor(
    @PublishedApi internal val handle: ThreadHandle
) {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        inline var name: String
            get() = getThreadName()
            set(value) {
                setThreadName(value)
            }

        @OptIn(ExperimentalForeignApi::class)
        inline val id: ULong
            get() = getThreadId()

        @OptIn(ExperimentalForeignApi::class)
        inline fun create(noinline function: () -> Unit): Thread = Thread(createThread(function))

        @OptIn(ExperimentalForeignApi::class)
        inline fun current(): Thread = Thread(currentThread())

        inline fun yield() = sched_yield()
    }

    @OptIn(ExperimentalForeignApi::class)
    inline fun join() = joinThread(handle)

    @OptIn(ExperimentalForeignApi::class)
    inline fun detach() = detachThread(handle)
}