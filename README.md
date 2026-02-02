# Filament

[![](https://git.karmakrafts.dev/kk/filament/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/filament/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.maven.apache.org%2Fmaven2%2Fdev%2Fkarmakrafts%2Ffilament%2Ffilament-core%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/introspekt/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Ffilament%2Ffilament-core%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/filament/-/packages)
[![](https://img.shields.io/badge/2.3.20--Beta2-blue?logo=kotlin&label=kotlin)](https://kotlinlang.org/)
[![](https://img.shields.io/badge/documentation-black?logo=kotlin)](https://docs.karmakrafts.dev/filament-core)

Common `Thread` class (and snychronization primitives) for Kotlin/Multiplatform.
This allows true, pure parallelism in Kotlin where controlling an OS thread directly is feasable.  
The library also offers various utilities and integration with other libraries.

### Features

- `Processor` as a general purpose abstraction for accessing CPU related information
    * Support for querying the logical core count
- `Thread` as a general purpose wrapper around system threads
    * `java.lang.Thread` on JVM/Android
    * [libpthread](https://www.gnu.org/software/hurd/libpthread.html) on Kotlin/Native targets
    * Unified support for thread names
    * Unified support for thread IDs
    * Support for setting thread affinity (pinning)
- `Mutex` as a simple mutex implementation
- `SharedMutex` as a reentrant read-write lock implementation
- `Executor` interface for bridging different APIs
- `ThreadPool` as a simple work-stealing thread pool implementation
    * Default implementation of `Executor`
- `Future` interface for unifying awaitable objects
- `CompletableFuture` type with atomic state as a default `Future` implementation
    * Create uncompleted futures with any default value
    * Create a value supplying task from an `Executor` and a block
- [java.util.concurrent](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html)
  integration
    * Use any filament `Executor` as a JVM `Executor`
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) integration
    * Use any `Executor` as a `CoroutineDispatcher`
    * Use any `Deferred<T>` as a `Future<T>`
    * Use `CompletableFuture.asyncSuspend` to create awaitable objects from a suspend block
    * Use `Future.awaitSuspend` to await any future by yielding the coroutine

More features may be added in the future. Contributions are welcome! :)

### How to use it

First, add the official Karma Krafts maven repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

Then add a dependency on the library in your buildscript:

```kotlin
kotlin {
    commonMain {
        dependencies {
            implementation("dev.karmakrafts.filament:filament-core:<version>")
        }
    }
}
```