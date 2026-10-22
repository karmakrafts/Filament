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
import dev.karmakrafts.conventions.kotlin.withCInterop
import dev.karmakrafts.conventions.kotlin.withDefaultDeviceTests
import dev.karmakrafts.conventions.kotlin.withDefaultHostTests
import dev.karmakrafts.conventions.kotlin.withJvm
import dev.karmakrafts.conventions.kotlin.withLinuxArm64StandaloneTests
import dev.karmakrafts.conventions.kotlin.withNative
import dev.karmakrafts.conventions.setProjectInfo
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

configureJava(rootProject.libs.versions.java)

configureDokka {
    withJava()
    withKotlin()
    withKotlinxCoroutines()
}

val platformPackage: GitLabPackage = gitlab().project(
    "kk/prebuilts/filament-platform"
).packageRegistry["generic/build", libs.versions.filamentPlatform]

platformPackage.registerCopyJvmJniLibraryTasks("platform", "filament-platform-jni")
platformPackage.registerCopyAndroidJniLibraryTasks("platform", "filament-platform-jni")

kotlin {
    defaultCompilerOptions()
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    withSourcesJar()
    withJvm()
    withAndroidLibrary(namespace = "$group.core", minSdk = provider { "26" }) {
        withDefaultHostTests()
        withDefaultDeviceTests()
    }
    withNative {
        withCInterop("filament-platform", platformPackage)
    }
    withLinuxArm64StandaloneTests()
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
                    exclude(group = "net.java.dev.jna", module = "jna-jpms")
                    exclude(group = "net.java.dev.jna", module = "jna-platform")
                    exclude(group = "net.java.dev.jna", module = "jna-platform-jpms")
                    exclude(group = "net.java.dev.jna", module = "jna-runtime")
                    exclude(group = "net.java.dev.jna", module = "jna-runtime-jpms")
                }
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
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
        named("androidHostTest") {
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
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