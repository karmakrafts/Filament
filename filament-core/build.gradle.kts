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

import dev.karmakrafts.conventions.asAAR
import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.defaultDokkaConfig
import dev.karmakrafts.conventions.kotlin.defaultCompilerOptions
import dev.karmakrafts.conventions.kotlin.withAndroidLibrary
import dev.karmakrafts.conventions.kotlin.withJvm
import dev.karmakrafts.conventions.kotlin.withNative
import dev.karmakrafts.conventions.setProjectInfo
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

configureJava(rootProject.libs.versions.java)
defaultDokkaConfig()

fun KotlinNativeTarget.pthreadInterop() {
    compilations.getByName("main") {
        cinterops {
            register("pthread")
        }
    }
}

kotlin {
    defaultCompilerOptions()
    withSourcesJar()
    withJvm()
    withAndroidLibrary()
    withNative()
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withAndroidLibrary()
            }
        }
    }
    sourceSets {
        nativeMain {
            dependencies {
                implementation(libs.stately.concurrent.collections)
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
    }
}

publishing {
    setProjectInfo(
        name = "Filament Core",
        description = "Common Thread class (and snychronization primitives) for Kotlin Multiplatform",
        url = "https://git.karmakrafts.dev/kk/filament"
    )
}