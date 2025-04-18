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

@file:JvmName("Lockable$")

package dev.karmakrafts.filament

import kotlin.jvm.JvmName

// TODO: document this
interface Lockable {
    fun lock()

    fun tryLock(): Boolean

    fun unlock()
}

// TODO: document this
inline fun <reified R> Lockable.guarded(closure: () -> R): R {
    return try {
        lock()
        closure()
    }
    finally {
        unlock()
    }
}

// TODO: document this
inline fun <reified R> Lockable.tryGuarded(defaultValue: R, closure: () -> R): R {
    if (!tryLock()) return defaultValue
    return try {
        closure()
    }
    finally {
        unlock()
    }
}