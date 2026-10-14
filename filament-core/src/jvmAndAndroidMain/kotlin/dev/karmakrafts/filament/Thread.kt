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

@file:JvmName("ThreadImpl")

package dev.karmakrafts.filament

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import java.lang.Thread as JavaThread

private val internalThreadId: AtomicInt = AtomicInt(1) // Skip 0 since that's always main

internal val threadAffinity: ThreadLocal<Int> = ThreadLocal.withInitial { Thread.NO_AFFINITY }

internal actual fun currentThread(): Thread = JvmThread(JavaThread.currentThread(), threadAffinity.get())

private class JvmThread( // @formatter:off
    val handle: JavaThread,
    override val affinity: Int
) : Thread { // @formatter:on
    override val isAlive: Boolean
        get() = handle.isAlive
    override val isDetached: Boolean
        get() = handle.isDaemon

    override fun join() = handle.join()

    override fun detach() {
        handle.isDaemon = true
    }
}

@PublishedApi
internal actual fun setThreadName(name: String?) {
    JavaThread.currentThread().apply {
        this.name = name ?: "Thread $id"
    }
}

@PublishedApi
internal actual fun getThreadName(): String {
    return JavaThread.currentThread().run {
        name ?: "Thread $id"
    }
}

@PublishedApi
internal actual fun getThreadId(): ULong {
    return JavaThread.currentThread().id.toULong()
}

internal actual fun sleepThread(millis: Long): Long {
    val lastTime = System.nanoTime()
    JavaThread.sleep(millis)
    return (System.nanoTime() - lastTime).nanoseconds.toLong(DurationUnit.MILLISECONDS)
}

internal actual fun yieldThread() {
    JavaThread.yield()
}

actual fun Thread( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> Unit
): Thread { // @formatter:on
    val actualStackSize = if (stackSize == Thread.DEFAULT_STACK_SIZE) 0 else stackSize
    val handle = JavaThread(null, function, "Thread ${internalThreadId.fetchAndIncrement()}", actualStackSize)
    val thread = JvmThread(handle, affinity)
    if (detached) thread.detach()
    thread.handle.start()
    threadAffinity.set(affinity)
    return thread
}