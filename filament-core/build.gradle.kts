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

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import dev.karmakrafts.conventions.GitLabPackage
import dev.karmakrafts.conventions.asAAR
import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.dokka.configureDokka
import dev.karmakrafts.conventions.dokka.withJava
import dev.karmakrafts.conventions.dokka.withKotlin
import dev.karmakrafts.conventions.dokka.withKotlinxCoroutines
import dev.karmakrafts.conventions.gitlab
import dev.karmakrafts.conventions.kotlin.defaultCompilerOptions
import dev.karmakrafts.conventions.kotlin.withAndroidLibrary
import dev.karmakrafts.conventions.kotlin.withJvm
import dev.karmakrafts.conventions.kotlin.withNative
import dev.karmakrafts.conventions.setProjectInfo
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import kotlin.io.path.div

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

val jvmPlatforms: List<Pair<String, String>> = listOf( // @formatter:off
    "windows-amd64" to "windowsAmd64",
    "linux-amd64" to "linuxAmd64",
    "linux-arm64" to "linuxArm64",
    "macos-arm64" to "macosArm64"
) // @formatter:on

infix fun <T, U, V> Pair<T, U>.to(value: V): Triple<T, U, V> = Triple(first, second, value)
val androidPlatforms: List<Triple<String, String, String>> = listOf( // @formatter:off
    "android-amd64" to "androidAmd64" to "x86_64",
    "android-arm64" to "androidArm64" to "arm64-v8a",
    "android-arm32" to "androidArm32" to "armeabi-v7a"
) // @formatter:on

val platformBinaries: Map<String, String> = mapOf(
    "windows-amd64" to "libfilament-platform.dll",
    "linux-amd64" to "libfilament-platform.so",
    "linux-arm64" to "libfilament-platform.so",
    "macos-arm64" to "libfilament-platform.dylib",
    "android-amd64" to "libfilament-platform.so",
    "android-arm64" to "libfilament-platform.so",
    "android-arm32" to "libfilament-platform.so"
)

val platformPackage: GitLabPackage = gitlab().project(
    "kk/prebuilts/filament-platform"
).packageRegistry["generic/build", libs.versions.filamentPlatform]

configureJava(rootProject.libs.versions.java)

configureDokka {
    withJava()
    withKotlin()
    withKotlinxCoroutines()
}

kotlin {
    defaultCompilerOptions()
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    withSourcesJar()
    withJvm()
    withAndroidLibrary("$group.core")
    withNative {
        compilations {
            named("main") {
                cinterops {
                    create("spin_loop_hint") // for Thread.spinLoopHint() on native
                    create("tsc") // for Processor.ticks and Processor.ticksSupported on native
                }
            }
        }
    }
    applyDefaultHierarchyTemplate {
        common {
            group("ios") { withIos() }
            group("tvos") { withTvos() }
            group("watchos") { withWatchos() }
            group("jvmAndAndroid") {
                withJvm()
                withAndroidLibrary()
            }
            group("apple") {
                group("appleNonDesktop") {
                    group("ios")
                    group("tvos")
                    group("watchos")
                }
            }
        }
    }
    sourceSets {
        nativeMain {
            dependencies {
                implementation(libs.kotlinx.atomicfu) // Used for ReentrantLock
            }
        }
        named("jvmAndAndroidMain") {
            dependencies {
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.io.bytestring)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                val oshiCore = libs.oshi.core.get()
                implementation("${oshiCore.module}:${oshiCore.version}") {
                    exclude(group = "net.java.dev.jna", module = "jna")
                    exclude(group = "net.java.dev.jna", module = "jna-platform")
                }
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
            resources.srcDir(project.layout.buildDirectory.dir("jvmNativesResources"))
        }
        androidMain {
            dependencies {
                implementation(libs.jna.asProvider().asAAR())
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

publishing {
    setProjectInfo(
        name = "Filament Core",
        description = "Common Thread class (and snychronization primitives) for Kotlin Multiplatform",
        url = "https://git.karmakrafts.dev/kk/filament"
    )
}

tasks {
    val jvmPlatformCopyTasks = registerCopyJvmJniLibraryTasks("platform", platformPackage, platformBinaries)
    named("jvmProcessResources") {
        dependsOn(jvmPlatformCopyTasks)
    }
    val androidPlatformCopyTasks = registerCopyAndroidJniLibraryTasks("platform", platformPackage, platformBinaries)
    afterEvaluate {
        named("mergeAndroidMainJniLibFolders") {
            dependsOn(androidPlatformCopyTasks)
        }
    }
}

fun TaskContainer.registerCopyJvmJniLibraryTasks(
    taskPrefix: String, pkg: GitLabPackage, binaryNames: Map<String, String>
): List<TaskProvider<Copy>> {
    val tasks = ArrayList<TaskProvider<Copy>>()
    for ((name, suffix) in jvmPlatforms) {
        val platformArtifact = pkg["build-$name-release.zip", suffix]
        val copyTask = register<Copy>("copy${taskPrefix.capitalized()}JniLibraries${suffix.capitalized()}") {
            description = "Extracts required JNI natives to the resource root of the JVM"
            group = "JNI Libraries"
            dependsOn(platformArtifact.extractTask)
            from((platformArtifact.outputDirectoryPath / binaryNames[name]!!).toFile())
            into(project.layout.buildDirectory.dir("jvmNativesResources/filament-natives/$name"))
            onlyIf { true } // Always copy these over when building/setting up
        }
        named("prepareKotlinIdeaImport") { dependsOn(copyTask) }
        named("assemble") { dependsOn(copyTask) }
        tasks += copyTask
    }
    return tasks
}

fun TaskContainer.registerCopyAndroidJniLibraryTasks(
    taskPrefix: String, pkg: GitLabPackage, binaryNames: Map<String, String>
): List<TaskProvider<Copy>> {
    val tasks = ArrayList<TaskProvider<Copy>>()
    for ((name, suffix, jniTarget) in androidPlatforms) {
        val platformArtifact = pkg["build-$name-release.zip", suffix]
        val copyTask = register<Copy>("copy${taskPrefix.capitalized()}JniLibraries${suffix.capitalized()}") {
            description = "Extracts required JNI natives to the resource root of Android"
            group = "JNI Libraries"
            dependsOn(platformArtifact.extractTask)
            from((platformArtifact.outputDirectoryPath / binaryNames[name]!!).toFile())
            into("src/androidMain/jniLibs/$jniTarget")
            onlyIf { true } // Always copy these over when building/setting up
        }
        named("prepareKotlinIdeaImport") { dependsOn(copyTask) }
        named("assemble") { dependsOn(copyTask) }
        tasks += copyTask
    }
    return tasks
}