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

package io.karma.filament

import java.lang.Thread as JavaThread

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

internal actual fun setThreadName(name: String?) {
    JavaThread.currentThread().apply {
        this.name = name ?: "Thread $id"
    }
}

internal actual fun getThreadName(): String {
    return JavaThread.currentThread().run {
        name ?: "Thread $id"
    }
}

internal actual fun getThreadId(): ULong {
    return JavaThread.currentThread().id.toULong()
}

internal actual fun suspendThread(millis: Long): Long {
    JavaThread.sleep(millis)
    return 0 // We always assume perfect suspend time for now
}

internal actual fun yieldThread() {
    JavaThread.yield()
}