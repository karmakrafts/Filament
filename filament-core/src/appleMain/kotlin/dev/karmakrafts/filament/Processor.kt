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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import platform.darwin.sysctlbyname
import platform.posix.size_tVar

@OptIn(UnsafeNumber::class)
private fun readSysctlString(name: String): String? = memScoped {
    val size = alloc<size_tVar> { value = 0U }
    if (sysctlbyname(name, null, size.ptr, null, 0U) != 0) return@memScoped null
    if (size.value.toULong() == 0UL || size.value > Int.MAX_VALUE.toULong()) return@memScoped null
    val capacity = size.value.toInt()
    val buffer = allocArray<ByteVar>(capacity)
    if (sysctlbyname(name, buffer, size.ptr, null, 0U) != 0) return@memScoped null
    if (size.value.toULong() == 0UL || size.value > capacity.toULong()) return@memScoped null
    buffer.readBytes(size.value.toInt()).decodeToString().substringBefore('\u0000')
}

private class MacosCpuInfo {
    private fun read(name: String): String? = readSysctlString(name)?.trim()?.takeIf { it.isNotEmpty() }

    val vendor: String by lazy {
        read("machdep.cpu.vendor") ?: if (model.startsWith("Apple ")) "Apple" else "Unknown"
    }

    val model: String by lazy { read("machdep.cpu.brand_string") ?: "Unknown" }
}

private val cpuInfo: MacosCpuInfo by lazy { MacosCpuInfo() }

internal actual fun getProcessorVendor(): String = cpuInfo.vendor
internal actual fun getProcessorModel(): String = cpuInfo.model