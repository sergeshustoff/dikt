# DI.kt
Simple DI for kotlin multiplatform

## Installation

Early alpha builds are published with jitpack.io

In settings.gradle add:

    pluginManagement {
        resolutionStrategy {
            eachPlugin {
                if (requested.id.toString() == "com.github.sergeshustoff.dikt") {
                    useModule("com.github.sergeshustoff.dikt:dikt-gradle-plugin:1.0.0-alpha2")
                }
            }
        }
        repositories {
            gradlePluginPortal()
            maven { url "https://jitpack.io" }
        }
    }

In build.gradle add jitpack repository to buildscript and allprojects:

    buildscript {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }

In module that you wish to use DI.kt add plugin:

    plugins {
        ...
        id 'com.github.sergeshustoff.dikt'
    }

If you use multiplatform plugin also add dependency for annotations:

    sourceSets {
        commonMain {
            dependencies {
                implementation 'com.github.sergeshustoff.dikt:dikt-runtime:1.0.0-alpha2'
            }
        }
    }

In gradle.properties disable incremental compilation, it's not supported yet:

    kotlin.incremental=false

## Usage

Create module and declare provided dependencies. Use @ByDi to generate functions bodies for you.

    @DiModule
    class CarModule(
        @ProvidesAllContent
        val externalDependency: Something,
    ) {
        @ByDi(cached = true) val someSingleton(): SomeSingleton
        @ByDi fun provideSomethingElse(): SomethingElse
    }
  
Under the hood primary constructor will be called for SomethingElse and SomeSingleton. If constructor requires some parameters - they will be retrieved form this module or from nested modules properties and functions.

## Annotations

### @ByDi

Magical annotation that tells compiler plugin to generate method body using returned type's primary constructor.
Function parameters are used as provided dependencies, as well as anything inside parameter of type annotated with @ProvidesAllContent or containing module.

#### Example:
    
    class Something(val name: String)

    @ByDi fun provideSomething(name: String): Something

Code above will be transformed to

    fun provideSomething(name: String) = Something(name)

#### cached = true

This parameter tells compiler plugin to create a lazy property and return value from it. Functions marked with @ByDi(cached=true) don't support parameters.

### @DiModule

Tells compiler plugin that all @ByDi methods in this class may use its methods and properties as dependencies. 
 
#### Example:

    class Something(val name: String)

    @DiModule
    class MyModule(val somethingName: String) {
        @ByDi fun provideSomething(): Something
    }

Example above will use somethingName property of MyModule to provide name parameter for Something constructor.

### @ProvideAllContent

Tells compiler that dependency marked with this annotation might provide all its properties and functions as dependency.

This annotation doesn't work recursively.

#### Example:

    class ExternalModule(val name: String)

    class Something(val name: String)

    @DiModule
    class MyModule(
        @ProvidesAllContent
        private val external: ExternalModule
    ) {
        @ByDi fun provideSomething(): Something // will call constructor using external.name as parameter
    }