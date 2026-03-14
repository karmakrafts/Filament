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

package dev.karmakrafts.filament

import dev.karmakrafts.filament.Thread.Companion.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal expect fun currentThread(): Thread

@PublishedApi
internal expect fun setThreadName(name: String?)

@PublishedApi
internal expect fun getThreadName(): String

@PublishedApi
internal expect fun getThreadId(): ULong

internal expect fun sleepThread(millis: Long): Long

internal expect fun yieldThread()

/**
 * Represents a thread of execution in a multiprocessing environment.
 * This interface provides a cross-platform API for thread management.
 */
interface Thread {
    companion object {
        // TODO: document this
        const val NO_AFFINITY: Int = -1

        // TODO: document this
        const val DEFAULT_STACK_SIZE: Long = -1L

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
        fun current(): Thread = currentThread()

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
        fun sleep(millis: Long): Long = sleepThread(millis)

        /**
         * Same as [sleep], except that it takes and returns a [Duration] for ease-of-use.
         */
        fun sleep(duration: Duration): Duration = sleep(duration.inWholeMilliseconds).milliseconds
    }

    // TODO: document this
    val affinity: Int

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

// TODO: document this
expect fun Thread(
    affinity: Int = Thread.NO_AFFINITY,
    stackSize: Long = Thread.DEFAULT_STACK_SIZE,
    detached: Boolean = false,
    function: () -> Unit
): Thread
