[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin)
[![gradle plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/io/github/sergeshustoff/dikt/dikt-gradle-plugin/maven-metadata.xml.svg?label=gradle%20plugin)](https://plugins.gradle.org/plugin/io.github.sergeshustoff.dikt)
[![IDEA plugin](https://img.shields.io/jetbrains/plugin/v/17533-di-kt.svg)](https://plugins.jetbrains.com/plugin/17533-di-kt)


# DI.kt
Simple DI with compile-time dependency graph validation for kotlin multiplatform

## Installation

In module that you wish to use DI.kt add plugin:

    plugins {
        ...
        id 'io.github.sergeshustoff.dikt' version '1.0.0-aplha5'
    }

Install [idea plugin](https://plugins.jetbrains.com/plugin/17533-di-kt) for better support, it will remove errors from ide for methods with generated body.

If runtime library isn't added to dependency by plugin ([bug](https://github.com/sergeshustoff/dikt/issues/2)), then add it manually:

    dependencies {
        ...
        implementation "com.github.sergeshustoff.dikt:dikt-annotations:1.0.0-alpha6"
    }

## Usage

Create module provided and declare provided dependencies. Use @Create, @Provide, @CreateSingle and @ProvideSingle to generate functions bodies for you. Use @UseModules and @UseConstructors to control how dependencies are provided and what classes can be created by constructors.

    class SomethingModule(
        val externalDependency: Something,
    ) {
        @CreateSingle fun someSingleton(): SomeSingleton
        @Create fun provideSomethingElse(): SomethingElse
    }
  
Under the hood primary constructor will be called for SomethingElse and SomeSingleton. If constructor requires some parameters - they will be retrieved form this module or from nested modules properties and functions.

## Annotations

### @Create

Magical annotation that tells compiler plugin to generate method body using returned type's primary constructor.
Function parameters are used as provided dependencies, as well as anything inside parameters of types provided in @UseModules annotation and anything inside containing class.

#### Example:
    
    class Something(val name: String)

    @Create fun provideSomething(name: String): Something

Code above will be transformed to

    fun provideSomething(name: String) = Something(name)

### @Provide

Tells compiler plugin to generate method body that returns value of specified type retrieved from dependencies. It's useful when we need to elevate dependencies from nested modules.
Doesn't call constructor for returned type (unless listed in @UseConstructors).

#### Example:

    class Something(val name: String)

    class ExternalModule(
        val something: Something
    )

    @UseModules(ExternalModule::class)
    class MyModule(val external: ExternalModule) {
        @Provide fun provideSomething(): Something
    }

### @CreateSingle and @ProvideSingle

Same as @Create and @Provide, but creates a lazy property and return value from it. Functions marked with @CreateSingle and @ProvideSingle don't support parameters.

### @UseConstructors

Dependencies of types listed in this annotation parameters will be provided by constructor when required.
Might be applied to the whole module or to a single function.

#### Example:

    class SomeDependency

    class Something(val dependency: SomeDependency)

    @UseConstructors(SomeDependency::class)
    class MyModule {
        @Create fun provideSomething(): Something
    }

### @UseModules

When applied to module all dependencies of types listed in this annotation parameters will provide all its type visible properties and functions as dependency.
Dependencies of listed types should be available in module.

WARNING: This annotation doesn't work recursively.

#### Example:

    class ExternalModule(val name: String)

    class Something(val name: String)

    @UseModules(ExternalModule::class)
    class MyModule(
        private val external: ExternalModule
    ) {
        @Create fun provideSomething(): Something // will call constructor using external.name as parameter
    }