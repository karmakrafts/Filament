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

import java.util.concurrent.Executor as JavaExecutor

/**
 * Converts a Java Executor to a Filament Executor.
 * This extension function allows Java Executors to be used in contexts where a Filament Executor is required.
 *
 * @return A Filament Executor that delegates task execution to this Java Executor.
 */
fun JavaExecutor.asFilamentExecutor(): Executor = Executor { block ->
    execute(block)
}

/**
 * Converts a Filament Executor to a Java Executor.
 * This extension function allows Filament Executors to be used in contexts where a Java Executor is required.
 *
 * @return A Java Executor that delegates task execution to this Filament Executor.
 */
fun Executor.asJavaExecutor(): JavaExecutor = JavaExecutor { block ->
    enqueueTask(block::run)
}
