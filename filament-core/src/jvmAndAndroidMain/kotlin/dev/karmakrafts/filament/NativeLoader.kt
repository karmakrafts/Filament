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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import oshi.util.PlatformEnum
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.uuid.Uuid
import java.lang.Thread as JavaThread

@Serializable
internal data class NativeLoaderArchitectureConfig( // @formatter:off
    val resources: List<String> = emptyList(),
    val systemLibs: List<String> = emptyList()
) // @formatter:on

@Serializable
internal data class NativeLoaderPlatformConfig( // @formatter:off
    val architectures: Map<String, NativeLoaderArchitectureConfig> = emptyMap()
) // @formatter:on

@Serializable
internal data class NativeLoaderConfig( // @formatter:off
    val version: Int = VERSION,
    val platforms: Map<PlatformEnum, NativeLoaderPlatformConfig> = emptyMap()
) { // @formatter:on
    companion object {
        const val VERSION: Int = 1
    }
}

internal object NativeLoader {
    private val json: Json = Json { ignoreUnknownKeys = true }
    private val isLoaded: AtomicBoolean = AtomicBoolean(false)

    @Suppress("UnsafeDynamicallyLoadedCode")
    @OptIn(ExperimentalSerializationApi::class, ExperimentalPathApi::class)
    fun ensureLoaded() {
        if (!isLoaded.compareAndSet(expectedValue = false, newValue = true)) return
        val config = NativeLoader::class.java.getResourceAsStream("/filament-natives/config.json")?.use {
            json.decodeFromStream<NativeLoaderConfig>(it)
        }!!
        check(config.version == NativeLoaderConfig.VERSION) {
            "Mismatched NativeLoader config version: expected ${NativeLoaderConfig.VERSION} but got ${config.version}"
        }
        val currentArch = System.getProperty("os.arch") ?: error("os.arch is not set, cannot determine architecture")
        val platformConfig = config.platforms[PlatformEnum.getCurrentPlatform()]!!
        for ((compoundArch, archConfig) in platformConfig.architectures) {
            val architectures = compoundArch.split(",") // We can match multiple architectures
            if (currentArch !in architectures) continue
            for (library in archConfig.systemLibs) try {
                System.loadLibrary(library)
            } catch (error: Throwable) {
                System.err.println("Could not load Filament system library $library: ${error.stackTraceToString()}")
            }
            if (archConfig.resources.isNotEmpty()) {
                val userHome = Path(System.getProperty("user.home") ?: ".").absolute()
                val filamentHome = userHome / ".filament"
                val tempDir = filamentHome / Uuid.random().toHexString()
                tempDir.createDirectories()
                for (resource in archConfig.resources) {
                    val fileName = resource.substringAfterLast('/')
                    val tempFile = tempDir / fileName
                    try {
                        tempFile.deleteIfExists()
                        NativeLoader::class.java.getResourceAsStream("/$resource")!!
                            .asSource()
                            .buffered()
                            .use { source ->
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
    }
}
