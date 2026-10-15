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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class MacosProcessorTest {
    @Test
    fun `Read Intel processor information and trim whitespace`() {
        val info = MacosCpuInfo { name ->
            when (name) {
                "machdep.cpu.vendor" -> "  GenuineIntel  "
                "machdep.cpu.brand_string" -> "  Intel(R) Core(TM) i7 CPU  "
                else -> fail("Unexpected sysctl key: $name")
            }
        }
        assertEquals("GenuineIntel", info.vendor)
        assertEquals("Intel(R) Core(TM) i7 CPU", info.model)
    }

    @Test
    fun `Infer Apple vendor when the vendor key is unavailable`() {
        val info = MacosCpuInfo { name ->
            if (name == "machdep.cpu.brand_string") "Apple M3 Pro" else null
        }
        assertEquals("Apple", info.vendor)
        assertEquals("Apple M3 Pro", info.model)
    }

    @Test
    fun `Prefer the reported vendor over the model fallback`() {
        val info = MacosCpuInfo { name ->
            when (name) {
                "machdep.cpu.vendor" -> "Reported vendor"
                "machdep.cpu.brand_string" -> "Apple M3 Pro"
                else -> null
            }
        }
        assertEquals("Reported vendor", info.vendor)
    }

    @Test
    fun `Missing processor information is unknown`() {
        val info = MacosCpuInfo { null }
        assertEquals("Unknown", info.vendor)
        assertEquals("Unknown", info.model)
    }

    @Test
    fun `Empty and blank processor information is unknown`() {
        for (value in listOf("", " \t\n")) {
            val info = MacosCpuInfo { value }
            assertEquals("Unknown", info.vendor)
            assertEquals("Unknown", info.model)
        }
    }

    @Test
    fun `Missing model does not discard the vendor`() {
        val info = MacosCpuInfo { name ->
            if (name == "machdep.cpu.vendor") "GenuineIntel" else null
        }
        assertEquals("GenuineIntel", info.vendor)
        assertEquals("Unknown", info.model)
    }

    @Test
    fun `Non Apple models do not imply an Apple vendor`() {
        val info = MacosCpuInfo { name ->
            if (name == "machdep.cpu.brand_string") "Intel(R) Core(TM) i7 CPU" else null
        }
        assertEquals("Unknown", info.vendor)
        assertEquals("Intel(R) Core(TM) i7 CPU", info.model)
    }

    @Test
    fun `Processor queries are cached including missing values`() {
        for (model in listOf("Apple M3 Pro", null)) {
            val queries = mutableMapOf<String, Int>()
            val info = MacosCpuInfo { name ->
                queries[name] = queries.getOrElse(name) { 0 } + 1
                if (name == "machdep.cpu.brand_string") model else null
            }
            repeat(100) {
                assertEquals(if (model == null) "Unknown" else "Apple", info.vendor)
                assertEquals(model ?: "Unknown", info.model)
            }
            assertEquals(mapOf("machdep.cpu.vendor" to 1, "machdep.cpu.brand_string" to 1), queries)
        }
    }

    @Test
    fun `Read a sysctl string`() {
        assertEquals("Darwin", readSysctlString("kern.ostype"))
    }

    @Test
    fun `Unavailable sysctl string returns null`() {
        assertNull(readSysctlString("machdep.cpu.filament_nonexistent_key"))
    }

    @Test
    fun `Processor properties match sysctl and remain stable`() {
        val info = MacosCpuInfo(::readSysctlString)
        assertTrue(Processor.vendor.isNotBlank())
        assertTrue(Processor.model.isNotBlank())
        repeat(100) {
            assertEquals(info.vendor, getProcessorVendor())
            assertEquals(info.model, getProcessorModel())
            assertEquals(info.vendor, Processor.vendor)
            assertEquals(info.model, Processor.model)
        }
    }

    @Test
    fun `Processor identity is consistent across threads`() {
        val expected = Processor.vendor to Processor.model
        repeat(4) {
            val result = Thread<Pair<String, String>> {
                Processor.vendor to Processor.model
            }.awaitResult()
            assertEquals(expected, result)
        }
    }
}