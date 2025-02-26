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

import java.util.concurrent.locks.ReentrantLock

internal data class JvmMutexHandle(
    val value: ReentrantLock
) : MutexHandle

internal actual fun createMutex(): MutexHandle = JvmMutexHandle(ReentrantLock())

internal actual fun lockMutex(handle: MutexHandle) {
    require(handle is JvmMutexHandle)
    handle.value.lock()
}

internal actual fun tryLockMutex(handle: MutexHandle): Boolean {
    require(handle is JvmMutexHandle)
    return handle.value.tryLock()
}

internal actual fun unlockMutex(handle: MutexHandle) {
    require(handle is JvmMutexHandle)
    return handle.value.unlock()
}