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

@file:OptIn(ExperimentalNativeApi::class)

package dev.karmakrafts.filament

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

internal interface MutexHandle : AutoCloseable

internal interface SharedMutexHandle : AutoCloseable

internal class BoxedMutexHandle(
    value: MutexHandle
) {
    private val _value: GCBox<MutexHandle> = GCBox(value)

    @Suppress("UNUSED")
    private val cleaner: Cleaner = createCleaner(_value, GCBox<MutexHandle>::drop)

    val value: MutexHandle
        get() = _value.value
}

internal class BoxedSharedMutexHandle(
    value: SharedMutexHandle
) {
    private val _value: GCBox<SharedMutexHandle> = GCBox(value)

    @Suppress("UNUSED")
    private val cleaner: Cleaner = createCleaner(_value, GCBox<SharedMutexHandle>::drop)

    val value: SharedMutexHandle
        get() = _value.value
}