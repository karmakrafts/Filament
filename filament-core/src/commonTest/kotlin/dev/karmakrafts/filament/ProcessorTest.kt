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

class ProcessorTest {
    @Test
    fun `Check logical core count`() {
        assertTrue(Processor.logicalCores > 0)
    }

    @Test
    fun `Processor information is stable across repeated reads`() {
        val logicalCores = Processor.logicalCores
        val is64Bit = Processor.is64Bit
        repeat(100) {
            assertEquals(logicalCores, Processor.logicalCores)
            assertEquals(is64Bit, Processor.is64Bit)
        }
    }

    @Test
    fun `Processor information is consistent across threads`() {
        val logicalCores = Processor.logicalCores
        val is64Bit = Processor.is64Bit
        repeat(4) {
            val result = Thread<Pair<Int, Boolean>> {
                Processor.logicalCores to Processor.is64Bit
            }.awaitResult()
            assertTrue(result.first > 0)
            assertEquals(logicalCores, result.first)
            assertEquals(is64Bit, result.second)
        }
    }
}