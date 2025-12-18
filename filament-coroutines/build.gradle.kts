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

import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.setProjectInfo

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

configureJava(rootProject.libs.versions.java)

kotlin {
    withSourcesJar(true)
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    androidLibrary {
        namespace = "$group.${rootProject.name}"
        compileSdk = libs.versions.androidCompileSDK.get().toInt()
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
    }
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
    jvm()
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(projects.filamentCore)
                api(libs.kotlinx.coroutines.core)
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
        name = "Filament Coroutines",
        description = "kotlinx.coroutines integration for the Filament threading library",
        url = "https://git.karmakrafts.dev/kk/filament"
    )
}