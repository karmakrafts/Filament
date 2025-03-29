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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_t
import platform.posix.sched_yield

internal data class NativeThreadHandle(
    val value: pthread_t
) : ThreadHandle

@OptIn(ExperimentalForeignApi::class)
internal actual fun yieldThread() {
    sched_yield()
}