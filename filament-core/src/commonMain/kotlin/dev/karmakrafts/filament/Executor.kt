/*
 * Copyright 2025 Karma Krafts
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
 * An interface for executing tasks asynchronously.
 * Implementations of this interface provide a way to execute tasks in a separate thread or thread pool.
 */
interface Executor : AutoCloseable {
    /**
     * Submits a task for execution.
     * The task will be executed at some point in the future, depending on the implementation.
     *
     * @param task The function to be executed.
     */
    fun enqueueTask(task: () -> Unit)
}
