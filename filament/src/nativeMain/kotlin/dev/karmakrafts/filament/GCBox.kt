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

import co.touchlab.stately.collections.SharedLinkedList
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.posix.atexit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalNativeApi::class, ExperimentalAtomicApi::class)
internal data class GCBox<T>(
    val value: T,
    private val dropAction: (T) -> Unit
) {
    @OptIn(ExperimentalForeignApi::class)
    companion object {
        @OptIn(ExperimentalNativeApi::class)
        private val boxes: SharedLinkedList<WeakReference<GCBox<*>>> = SharedLinkedList()

        init {
            atexit(staticCFunction<Unit> {
                while (GCBox.boxes.isNotEmpty()) {
                    GCBox.boxes.removeFirst().value?.drop()
                }
            })
        }
    }

    var isDropped: AtomicBoolean = AtomicBoolean(false)

    init {
        boxes += WeakReference(this)
    }

    fun drop() {
        if (!isDropped.compareAndSet(expectedValue = false, newValue = true)) return
        dropAction(value)
    }
}