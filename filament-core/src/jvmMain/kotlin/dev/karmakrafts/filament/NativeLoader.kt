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

import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import java.lang.Thread as JavaThread

@Suppress("UnsafeDynamicallyLoadedCode")
@OptIn(ExperimentalPathApi::class)
internal actual fun loadExtraLibraries(config: NativeLoaderArchitectureConfig) {
    if (config.resources.isNotEmpty()) {
        val tempDir = Files.createTempDirectory("filament")
        for (resource in config.resources) {
            val fileName = resource.substringAfterLast('/')
            val tempFile = tempDir / fileName
            try {
                tempFile.deleteIfExists()
                NativeLoader::class.java.getResourceAsStream("/$resource")!!.asSource().buffered().use { source ->
                    FileOutputStream(File(tempFile.toString())).asSink().use { sink ->
                        source.transferTo(sink)
                    }
                }
                System.load(tempFile.absolutePathString())
            } catch (error: Throwable) {
                System.err.println("Could not load Filament platform library $resource: ${error.stackTraceToString()}")
            }
        }
        Runtime.getRuntime().addShutdownHook(JavaThread {
            tempDir.deleteRecursively()
        })
    }
}