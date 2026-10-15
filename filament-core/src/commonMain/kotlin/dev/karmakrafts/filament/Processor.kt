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

/**
 * Provides information about the system's processor.
 *
 * This object allows access to various processor-related properties
 * in a platform-independent way.
 */
expect object Processor {
    /**
     * Returns the processor vendor reported by the platform.
     *
     * The format is platform-dependent and may be an identifier rather than a display name.
     * An unknown value may be reported when the vendor cannot be determined.
     *
     * @return The platform's processor vendor string.
     */
    val vendor: String

    /**
     * Returns the processor model reported by the platform.
     *
     * The format is platform-dependent and may be a model identifier or a descriptive name.
     * An unknown value may be reported when the model cannot be determined.
     *
     * @return The platform's processor model string.
     */
    val model: String

    /**
     * Indicates whether the platform supports reading the processor counter.
     *
     * Check this property before accessing [ticks]. Support depends on the processor architecture
     * and the counter access provided by the platform.
     *
     * @return `true` if processor ticks are supported, `false` otherwise.
     */
    @DelicateFilamentApi
    val ticksSupported: Boolean

    /**
     * Returns the current value of the processor's hardware counter.
     *
     * This is a raw counter with a platform-dependent origin, not a wall-clock timestamp or
     * a count of nanoseconds. No counter frequency is provided for converting ticks to elapsed time.
     * Counter behavior and synchronization between processor cores are platform-dependent;
     * readings from different cores are not guaranteed to be comparable.
     *
     * Only read this property when [ticksSupported] is `true`. Access on an unsupported
     * platform may cause a hardware fault rather than a Kotlin exception.
     *
     * @return The current processor counter value in ticks.
     */
    @DelicateFilamentApi
    val ticks: ULong

    /**
     * Indicates whether the processor architecture is 64-bit.
     *
     * @return `true` if the processor is 64-bit, `false` otherwise.
     */
    val is64Bit: Boolean

    /**
     * Returns the number of logical processor cores available to the runtime.
     *
     * This value may be different from the number of physical cores if the processor
     * supports technologies like hyperthreading or similar features.
     *
     * @return The number of logical processor cores.
     */
    val logicalCores: Int
}
