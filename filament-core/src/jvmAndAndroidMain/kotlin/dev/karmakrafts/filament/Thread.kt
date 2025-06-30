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

package dev.karmakrafts.filament

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import java.lang.Thread as JavaThread

@PublishedApi
internal actual val threadSupportsAffinity: Boolean = false

internal data class JvmThreadHandle(
    val value: JavaThread
) : ThreadHandle

internal actual fun currentThread(): ThreadHandle {
    return JvmThreadHandle(JavaThread.currentThread())
}

internal actual fun createThread(function: () -> Unit): ThreadHandle {
    return JvmThreadHandle(JavaThread(function).apply {
        start()
    })
}

internal actual fun joinThread(handle: ThreadHandle) {
    require(handle is JvmThreadHandle)
    handle.value.join()
}

internal actual fun detachThread(handle: ThreadHandle) {
    require(handle is JvmThreadHandle)
    handle.value.isDaemon = true
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

internal actual fun suspendThread(millis: Long): Long {
    val lastTime = System.nanoTime()
    JavaThread.sleep(millis)
    return (System.nanoTime() - lastTime).nanoseconds.toLong(DurationUnit.MILLISECONDS)
}

internal actual fun yieldThread() {
    JavaThread.yield()
}

internal actual fun setThreadAffinity(vararg logicalCores: Int) = Unit