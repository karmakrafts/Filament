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

@PublishedApi
internal expect val isProcessor64Bit: Boolean

@PublishedApi
internal expect val logicalProcessorCount: Int

/**
 * Provides information about the system's processor.
 * 
 * This object allows access to various processor-related properties
 * in a platform-independent way.
 */
object Processor {
    /**
     * Indicates whether the processor architecture is 64-bit.
     * 
     * @return `true` if the processor is 64-bit, `false` otherwise.
     */
    inline val is64Bit: Boolean
        get() = isProcessor64Bit

    /**
     * Returns the number of logical processor cores available to the runtime.
     * 
     * This value may be different from the number of physical cores if the processor
     * supports technologies like hyperthreading or similar features.
     * 
     * @return The number of logical processor cores.
     */
    inline val logicalCores: Int
        get() = logicalProcessorCount
}
