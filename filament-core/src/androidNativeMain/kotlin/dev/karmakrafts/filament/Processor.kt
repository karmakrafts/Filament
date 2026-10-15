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
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

private data class CpuInfo(val lines: List<String>) {
    operator fun get(name: String): String? {
        val line = lines.find { line -> line.substringBefore(':').trim() == name } ?: return null
        return line.substringAfter(':', "").trim().takeIf { it.isNotEmpty() }
    }

    val vendor: String by lazy { this["vendor_name"] ?: this["vendor_id"] ?: "Unknown" }

    val model: String by lazy {
        this["model_name"] ?: this["model name"] ?: this["Hardware"] ?: this["Processor"] ?: "Unknown"
    }
}

private fun readCpuInfo(): CpuInfo = memScoped {
    val file = fopen("/proc/cpuinfo", "r") ?: return@memScoped CpuInfo(emptyList())
    try {
        val buffer = allocArray<ByteVar>(4096)
        val lines = ArrayList<String>()
        while (fgets(buffer, 4096, file) != null) {
            lines += buffer.toKStringFromUtf8()
        }
        CpuInfo(lines)
    }
    finally {
        fclose(file)
    }
}

private val cpuInfo: CpuInfo by lazy(::readCpuInfo)

internal actual fun getProcessorVendor(): String = cpuInfo.vendor
internal actual fun getProcessorModel(): String = cpuInfo.model