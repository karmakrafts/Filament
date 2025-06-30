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

import platform.posix.pthread_mutex_t
import platform.posix.pthread_rwlock_t
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

internal expect fun destroyMutex(handle: pthread_mutex_t)

internal expect fun destroySharedMutex(handle: pthread_rwlock_t)

@OptIn(ExperimentalNativeApi::class)
internal class NativeMutexHandle(
    value: pthread_mutex_t
) : MutexHandle {
    private val _value: GCBox<pthread_mutex_t> = GCBox(value, ::destroyMutex)

    @Suppress("UNUSED")
    private val cleaner: Cleaner = createCleaner(_value, GCBox<pthread_mutex_t>::drop)

    val value: pthread_mutex_t
        get() = _value.value
}

@OptIn(ExperimentalNativeApi::class)
internal class NativeSharedMutexHandle(
    value: pthread_rwlock_t
) : SharedMutexHandle {
    private val _value: GCBox<pthread_rwlock_t> = GCBox(value, ::destroySharedMutex)

    @Suppress("UNUSED")
    private val cleaner: Cleaner = createCleaner(_value, GCBox<pthread_rwlock_t>::drop)

    val value: pthread_rwlock_t
        get() = _value.value
}