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

import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.defaultDokkaConfig
import dev.karmakrafts.conventions.kotlin.defaultCompilerOptions
import dev.karmakrafts.conventions.kotlin.withAndroidLibrary
import dev.karmakrafts.conventions.kotlin.withJvm
import dev.karmakrafts.conventions.kotlin.withNative
import dev.karmakrafts.conventions.setProjectInfo

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

configureJava(rootProject.libs.versions.java)
defaultDokkaConfig()

kotlin {
    defaultCompilerOptions()
    withSourcesJar()
    withAndroidLibrary()
    withNative()
    withJvm()
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