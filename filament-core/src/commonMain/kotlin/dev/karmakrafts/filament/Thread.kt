/*
 * Copyright 2026 Karma Krafts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("Thread$")

package dev.karmakrafts.filament

import kotlin.jvm.JvmName

internal interface ThreadHandle

internal expect fun currentThread(): ThreadHandle

internal expect fun createThread(function: () -> Unit): ThreadHandle

internal expect fun joinThread(handle: ThreadHandle)

internal expect fun detachThread(handle: ThreadHandle)

@PublishedApi
internal expect fun setThreadName(name: String?)

@PublishedApi
internal expect fun getThreadName(): String

@PublishedApi
internal expect fun getThreadId(): ULong

internal expect fun suspendThread(millis: Long): Long

internal expect fun yieldThread()

internal expect fun setThreadAffinity(vararg logicalCores: Int)

internal expect fun isThreadAlive(handle: ThreadHandle): Boolean

internal expect fun isThreadDetached(handle: ThreadHandle): Boolean

@PublishedApi
internal expect val threadSupportsAffinity: Boolean

/**
 * Represents a thread of execution in a multiprocessing environment.
 * This interface provides a cross-platform API for thread management.
 */
interface Thread {
    companion object {
        /**
         * Indicates whether the current platform supports thread affinity.
         * Thread affinity allows binding a thread to specific CPU cores.
         *
         * @return True if thread affinity is supported, false otherwise.
         */
        inline val supportsAffinity: Boolean
            get() = threadSupportsAffinity

        /**
         * Gets or sets the name of the current thread.
         * Thread names are useful for debugging and logging purposes.
         */
        inline var name: String
            get() = getThreadName()
            set(value) {
                setThreadName(value)
            }

        /**
         * Gets the unique identifier of the current thread.
         *
         * @return The thread ID as an unsigned long integer.
         */
        inline val id: ULong
            get() = getThreadId()

        /**
         * Gets a reference to the current thread.
         *
         * @return A Thread object representing the current thread.
         */
        fun current(): Thread = ThreadImpl(currentThread())

        /**
         * Yields the current thread's execution, allowing other threads to execute.
         * This is a hint to the scheduler that the current thread is willing to yield its current use of the CPU.
         */
        fun yield() = yieldThread()

        /**
         * Causes the current thread to sleep for the specified number of milliseconds.
         * The thread will not execute during this time unless interrupted.
         *
         * @param millis The number of milliseconds to sleep.
         * @return The actual time slept in milliseconds, which may differ from the requested time.
         */
        fun sleep(millis: Long): Long = suspendThread(millis)

        /**
         * Sets the affinity of the current thread to the specified logical CPU cores.
         * This operation is only effective if thread affinity is supported on the current platform.
         *
         * @param logicalCores The indices of the logical CPU cores to which the thread should be bound.
         */
        fun setAffinity(vararg logicalCores: Int) = setThreadAffinity(*logicalCores)
    }

    /**
     * True if this thread is running and hasn't been joined.
     */
    val isAlive: Boolean

    /**
     * True if this thread was detached from the parent thread,
     * meaning it will keep running even if the creating thread terminates.
     */
    val isDetached: Boolean

    /**
     * Waits for this thread to die.
     * The calling thread will block until this thread has completed its execution.
     */
    fun join()

    /**
     * Detaches this thread, allowing it to run independently of the parent thread.
     * A detached thread will continue to run even after the parent thread has completed.
     */
    fun detach()
}

private class ThreadImpl(
    private val handle: ThreadHandle
) : Thread {
    override val isAlive: Boolean
        get() = isThreadAlive(handle)

    override val isDetached: Boolean
        get() = isThreadDetached(handle)

    override fun join() = joinThread(handle)

    override fun detach() = detachThread(handle)
}

/**
 * Creates and starts a new thread that executes the specified function.
 *
 * This is a factory function that provides a convenient way to create and start a thread
 * with a specific task to execute. The thread begins execution immediately after creation.
 *
 * @param function The function to be executed in the new thread.
 * @return A Thread object representing the newly created thread.
 */
fun Thread(function: () -> Unit): Thread = ThreadImpl(createThread(function))
