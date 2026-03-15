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

@file:OptIn(ExperimentalForeignApi::class)

package dev.karmakrafts.filament

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_t
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_t

internal value class AppleMutexHandle(
    val value: pthread_mutex_t
) : MutexHandle {
    override fun close() {
        pthread_mutex_destroy(value.ptr)
        nativeHeap.free(value.ptr)
    }
}

internal value class AppleSharedMutexHandle(
    val value: pthread_rwlock_t
) : SharedMutexHandle {
    override fun close() {
        pthread_rwlock_destroy(value.ptr)
        nativeHeap.free(value.ptr)
    }
}