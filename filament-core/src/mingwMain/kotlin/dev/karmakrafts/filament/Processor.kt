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
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.value
import kotlinx.cinterop.wcstr
import platform.windows.DWORDVar
import platform.windows.ERROR_SUCCESS
import platform.windows.HKEY_LOCAL_MACHINE
import platform.windows.RRF_RT_REG_SZ
import platform.windows.RegGetValueW

private fun readProcessorString(name: String): String? = memScoped {
    val key = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0".wcstr.ptr
    val valueName = name.wcstr.ptr
    val size = alloc<DWORDVar> { value = 0U }
    if (RegGetValueW(
            HKEY_LOCAL_MACHINE, key, valueName, RRF_RT_REG_SZ.toUInt(), null, null, size.ptr
        ) != ERROR_SUCCESS
    ) {
        return@memScoped null
    }
    if (size.value < 2U || size.value > Int.MAX_VALUE.toUInt() || size.value % 2U != 0U) return@memScoped null
    val capacity = size.value.toInt()
    val buffer = allocArray<UShortVar>(capacity / 2)
    if (RegGetValueW(
            HKEY_LOCAL_MACHINE, key, valueName, RRF_RT_REG_SZ.toUInt(), null, buffer, size.ptr
        ) != ERROR_SUCCESS
    ) {
        return@memScoped null
    }
    if (size.value < 2U || size.value > capacity.toUInt() || size.value % 2U != 0U) return@memScoped null
    buffer.toKStringFromUtf16().trim().takeIf { it.isNotEmpty() }
}

private class WindowsCpuInfo {
    val vendor: String by lazy { readProcessorString("VendorIdentifier") ?: "Unknown" }
    val model: String by lazy { readProcessorString("ProcessorNameString") ?: "Unknown" }
}

private val cpuInfo: WindowsCpuInfo by lazy { WindowsCpuInfo() }

internal actual fun getProcessorVendor(): String = cpuInfo.vendor

internal actual fun getProcessorModel(): String = cpuInfo.model