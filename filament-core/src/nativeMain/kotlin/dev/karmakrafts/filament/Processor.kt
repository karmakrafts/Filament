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

import dev.karmakrafts.filament.platform.filament_are_ticks_supported
import dev.karmakrafts.filament.platform.filament_get_ticks
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

internal expect fun getProcessorVendor(): String

internal expect fun getProcessorModel(): String

@OptIn(ExperimentalForeignApi::class)
actual object Processor {
    actual val is64Bit: Boolean
        get() = Platform.cpuArchitecture.bitness == 64

    actual val logicalCores: Int
        get() = Platform.getAvailableProcessors()

    actual val vendor: String
        get() = getProcessorVendor()

    actual val model: String
        get() = getProcessorModel()

    @DelicateFilamentApi
    actual val ticksSupported: Boolean
        get() = filament_are_ticks_supported()

    @DelicateFilamentApi
    actual val ticks: ULong
        get() = filament_get_ticks()
}
