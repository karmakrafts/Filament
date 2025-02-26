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

import platform.posix.pthread_mutex_t
import platform.posix.pthread_rwlock_t
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

internal expect fun destroyMutex(handle: pthread_mutex_t)

internal expect fun destroySharedMutex(handle: pthread_rwlock_t)

@OptIn(ExperimentalNativeApi::class)
internal data class NativeMutexHandle(
    val value: pthread_mutex_t
) : MutexHandle {
    init {
        createCleaner(value, ::destroyMutex)
    }
}

@OptIn(ExperimentalNativeApi::class)
internal data class NativeSharedMutexHandle(
    val value: pthread_rwlock_t
) : SharedMutexHandle {
    init {
        createCleaner(value, ::destroySharedMutex)
    }
}