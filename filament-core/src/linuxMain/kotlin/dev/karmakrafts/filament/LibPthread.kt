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

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import platform.posix.RTLD_NOW
import platform.posix.atexit
import platform.posix.cpu_set_t
import platform.posix.dlclose
import platform.posix.dlopen
import platform.posix.dlsym
import platform.posix.pthread_t
import platform.posix.size_t

private typealias _pthread_setaffinity_np = ( // @formatter:off
    thread: pthread_t,
    cpuSetSize: size_t,
    cpuSet: CValue<cpu_set_t>
) -> Int // @formatter:on

internal object LibPthread {
    private val libraryNames: Array<String> = arrayOf("libc.so", "libc.so.6")

    private val library: COpaquePointer? = run {
        var address: COpaquePointer? = null
        for (name in libraryNames) {
            address = dlopen(name, RTLD_NOW) ?: continue
        }
        address
    }

    private val _pthread_setaffinity_np: CPointer<CFunction<_pthread_setaffinity_np>>? =
        dlsym(library, "pthread_setaffinity_np")?.reinterpret()

    val isThreadAffinityAvailable: Boolean by lazy {
        library != null && _pthread_setaffinity_np != null
    }

    init {
        atexit(staticCFunction<Unit> {
            val self = LibPthread
            self.dispose()
        })
    }

    fun pthread_setaffinity_np( // @formatter:off
        thread: pthread_t,
        cpuSetSize: size_t,
        cpuSet: CValue<cpu_set_t>
    ): Int = checkNotNull(_pthread_setaffinity_np) { // @formatter:on
        "Thread affinity is not available on the current system"
    }(thread, cpuSetSize, cpuSet)

    fun CPU_SET(cpu: Int, set: CPointer<cpu_set_t>) {
        val index = cpu / ULong.SIZE_BITS
        val bit = cpu % ULong.SIZE_BITS
        val mask = set.pointed.__bits[index]
        set.pointed.__bits[index] = mask or (1UL shl bit)
    }

    private fun dispose() {
        dlclose(library)
    }
}