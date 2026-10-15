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

package dev.karmakrafts.filament

import oshi.SystemInfo

private val systemInfo: SystemInfo by lazy(::SystemInfo)

actual object Processor {
    actual val is64Bit: Boolean = System.getProperty("sun.arch.data.model")?.toIntOrNull() == 64
    actual val logicalCores: Int = Runtime.getRuntime().availableProcessors()

    actual val vendor: String
        get() = systemInfo.hardware.processor.processorIdentifier.vendor

    actual val model: String
        get() = systemInfo.hardware.processor.processorIdentifier.model

    @DelicateFilamentApi
    actual val ticksSupported: Boolean
        get() = FilamentPlatform.areTicksSupported()

    @DelicateFilamentApi
    actual val ticks: ULong
        get() = FilamentPlatform.getTicks().toULong()
}