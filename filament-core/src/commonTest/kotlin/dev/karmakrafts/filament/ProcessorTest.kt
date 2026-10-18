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
import kotlin.test.assertTrue

@OptIn(DelicateFilamentApi::class)
class ProcessorTest {
    private data class ProcessorInfo(
        val logicalCores: Int = Processor.logicalCores,
        val is64Bit: Boolean = Processor.is64Bit,
        val vendor: String = Processor.vendor,
        val model: String = Processor.model,
        val ticksSupported: Boolean = Processor.ticksSupported
    )

    @Test
    fun checkLogicalCoreCount() {
        assertTrue(Processor.logicalCores > 0)
    }

    @Test
    fun processorVendorIsNotBlank() {
        assertTrue(Processor.vendor.isNotBlank())
    }

    @Test
    fun processorModelIsNotBlank() {
        assertTrue(Processor.model.isNotBlank())
    }

    @Test
    fun processorTicksAdvanceWhenSupported() {
        if (!Processor.ticksSupported) return
        val start = Processor.ticks
        Thread.sleep(1)
        assertTrue(Processor.ticks > start, "The processor counter must advance during sleep")
    }

    @Test
    fun processorTicksAdvanceOnAWorkerThreadWhenSupported() {
        if (!Processor.ticksSupported) return
        val result = Thread<Pair<ULong, ULong>> {
            val start = Processor.ticks
            Thread.sleep(1)
            start to Processor.ticks
        }.awaitResult()
        assertTrue(result.second > result.first, "The processor counter must advance on a worker thread")
    }

    @Test
    fun processorInformationIsStableAcrossRepeatedReads() {
        val expected = ProcessorInfo()
        repeat(100) {
            assertEquals(expected, ProcessorInfo())
        }
    }

    @Test
    fun processorInformationIsConsistentAcrossThreads() {
        val expected = ProcessorInfo()
        repeat(4) {
            val result = Thread<ProcessorInfo> { ProcessorInfo() }.awaitResult()
            assertEquals(expected, result)
        }
    }
}