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

import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.setProjectInfo
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import java.time.ZonedDateTime

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    signing
    `maven-publish`
}

configureJava(rootProject.libs.versions.java)

fun KotlinNativeTarget.pthreadInterop() {
    compilations.getByName("main") {
        cinterops {
            val family = konanTarget.family
            if (family.isAppleFamily || family == Family.LINUX || family == Family.ANDROID) {
                val pthread by creating
            }
        }
    }
}

kotlin {
    withSourcesJar(true)
    mingwX64()
    linuxX64 { pthreadInterop() }
    linuxArm64 { pthreadInterop() }
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
    androidTarget {
        publishLibraryVariants("release")
    }
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    jvm()
    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting
        nativeMain {
            dependencies {
                implementation(libs.stately.concurrent.collections)
            }
        }
        val jvmAndAndroidMain by creating { dependsOn(commonMain) }
        jvmMain {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
        }
        androidMain {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                implementation("${libs.jna.asProvider().get()}@aar")
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "$group.${rootProject.name}"
    compileSdk = libs.versions.androidCompileSDK.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
    }
}

dokka {
    moduleName = project.name
    pluginsConfiguration {
        html {
            footerMessage = "(c) ${ZonedDateTime.now().year} Karma Krafts & associates"
        }
    }
}

val dokkaJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks {
    System.getProperty("publishDocs.root")?.let { docsDir ->
        register("publishDocs", Copy::class) {
            dependsOn(dokkaJar)
            mustRunAfter(dokkaJar)
            from(zipTree(dokkaJar.get().outputs.files.first()))
            into("$docsDir/${project.name}")
        }
    }
}

publishing {
    setProjectInfo(
        name = "Filament Core",
        description = "Common Thread class (and snychronization primitives) for Kotlin Multiplatform",
        url = "https://git.karmakrafts.dev/kk/filament"
    )
    publications.withType<MavenPublication> {
        artifact(dokkaJar)
    }
}