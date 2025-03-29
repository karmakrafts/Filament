# Filament

[![](https://git.karmakrafts.dev/kk/filament/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/filament/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Fpublish%2Fstaging%2Fmaven2%2Fdev%2Fkarmakrafts%2Ffilament%2Ffilament%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/filament/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Ffilament%2Ffilament%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/filament/-/packages)

Common `Thread` class (and snychronization primitives) for Kotlin/Multiplatform.
This allows true, pure parallelism in Kotlin where controlling an OS thread directly is feasable.

### Supported platforms

* Windows
* Linux
* macOS
* iOS
* watchOS
* tvOS
* Android
* Android Native
* JVM

### Features

- [x] `Thread` General purpose wrapper around system threads.
- [x] `Mutex` Simple mutex implementation.
- [x] `SharedMutex` Reentrant read-write lock implementation.
- [ ] `ConditionVar` Condition variables
- [ ] `ThreadAttribute` Thread attributes

More features may be added in the future. Contributions are welcome! :)

### How to use it

First, add the official Karma Krafts maven repository to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        // Snapshots are available from the Karma Krafts repository or Maven Central Snapshots
        maven("https://files.karmakrafts.dev/maven")
        maven("https://central.sonatype.com/repository/maven-snapshots")
        // Releases are mirrored to the central M2 repository
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // Snapshots are available from the Karma Krafts repository or Maven Central Snapshots
        maven("https://files.karmakrafts.dev/maven")
        maven("https://central.sonatype.com/repository/maven-snapshots")
        // Releases are mirrored to the central M2 repository
        mavenCentral()
    }
}
```

Then add a dependency on the library in your buildscript:

```kotlin
kotlin {
    commonMain {
        dependencies {
            implementation("dev.karmakrafts.filament:filament:<version>")
        }
    }
}
```