/*
 * Copyright 2025 Karma Krafts & associates
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

package io.karma.filament

import kotlin.jvm.JvmName

internal interface ThreadHandle

internal expect fun currentThread(): ThreadHandle

internal expect fun createThread(function: () -> Unit): ThreadHandle

internal expect fun joinThread(handle: ThreadHandle)

internal expect fun detachThread(handle: ThreadHandle)

internal expect fun setThreadName(name: String?)

internal expect fun getThreadName(): String

internal expect fun getThreadId(): ULong

internal expect fun suspendThread(millis: Long): Long

internal expect fun yieldThread()

interface Thread {
    companion object {
        var name: String
            get() = getThreadName()
            set(value) {
                setThreadName(value)
            }

        val id: ULong
            get() = getThreadId()

        fun current(): Thread = ThreadImpl(currentThread())

        fun yield() = yieldThread()

        fun sleep(millis: Long): Long = suspendThread(millis)
    }

    fun join()

    fun detach()
}

@Suppress("NOTHING_TO_INLINE")
private class ThreadImpl(
    private val handle: ThreadHandle
) : Thread {
    override fun join() = joinThread(handle)

    override fun detach() = detachThread(handle)
}

fun Thread(function: () -> Unit): Thread = ThreadImpl(createThread(function))