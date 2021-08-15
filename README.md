# DI.kt
Simple and powerful DI for kotlin multiplatform

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

Create module and declare provided dependencies. Use @ByDi to mark properties and functions to be autogenerated.

    @Module
    class CarModule(
        val externalDependency: Something,
    ) {
        @SingletonByDi val someSingleton(): SomeSingleton
        @ByDi fun provideSomethingElse(): SomethingElse
    }
  
Under the hood constructor with @Inject annotation will be called for SomethingElse and SomeSingleton. If there is no annotated constructor - primary constructor is used for direct dependency. If constructor requires some parameters - they will be retrieved form module properties, nested modules, module functions or created by a constructor with @Inject annotation.

## Warning

This library doesn't support incremental compilation yet, make sure you disabled it in your gradle.properties with 

    kotlin.incremental=false

## Annotations

### @ByDi

Magical annotation that tells compiler plugin to generate method body.
Function parameters and constructors marked with @Inject are used as provided dependencies, as well as anything inside parameter of type annotated with @Module or containing module.

Types returned from such functions don't need to be marked with @Inject.

Might be used for extension functions outside of module.
#### Example:
    
    class Something(val name: String)

    @ByDi fun provideSomething(name: String): Something

Code above for example will be transformed to

    fun provideSomething(name: String) = Something(name)
The transformation only affects compiled code, so your codebase is safe from the plugin)

### @SingletonByDi

Same as @ByDi, but creates lazy property and returns value from it. Doesn't support function parameters.

### @Module

Tells compiler plugin that provided dependency of that type should provide all its visible methods and properties as dependency. 
 
#### Example:

    class Something(val name: String)

    @Module
    class MyModule(val somethingName: String) {
        @SingletonByDi fun provideSomething(): Something
    }

Example above will use somethingName property of MyModule to provide name parameter for Something constructor.

### @SingletonIn(TModule::class)

This annotation set on a class tells compiler plugin to generate @ByDi function in module TModule for this type.

#### Example

For example this code is equivalent of the code from previous example:

    @SingletonIn(MyModule::class)
    class Something(val name: String)

    @Module
    class MyModule(val somethingName: String)

### @Inject

This annotation marks a constructor to be used to create type instance as dependency for something.
Set on type it marks primary constructor. 

Types with constructors marked by this annotation will be provided as dependency in any module with dependencies needed for calling this constructor.

#### Example:

    @Inject
    class Dependency()

    class Something(val dependency: Dependency)

    @Module
    class MyModule() {
        @ByDi fun provideSomething(): Something
    }

### @InjectNamed, @Named

Annotation @InjectNamed("key") is applied to method or constructor parameters and indicates that dependency provided for this parameter should be marked as @Named("key").

@Named("key") is applied to function parameters and, properties and functions in module, marking them for providing to parameters marked with @InjectNamed("key")