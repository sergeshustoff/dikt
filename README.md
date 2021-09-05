# DI.kt
Simple DI for kotlin multiplatform

## Installation

### publishing to gradle plugins portal is in progress...

In module that you wish to use DI.kt add plugin:

    plugins {
        ...
        id 'io.github.sergeshustoff.dikt' version '1.0.0-aplha4'
    }

## Usage

Create module and declare provided dependencies. Use @Create to generate functions bodies for you.

    @DiModule
    class CarModule(
        @ProvidesAll
        val externalDependency: Something,
    ) {
        @CreateCached val someSingleton(): SomeSingleton
        @Create fun provideSomethingElse(): SomethingElse
    }
  
Under the hood primary constructor will be called for SomethingElse and SomeSingleton. If constructor requires some parameters - they will be retrieved form this module or from nested modules properties and functions.

## Annotations

### @Create

Magical annotation that tells compiler plugin to generate method body using returned type's primary constructor.
Function parameters are used as provided dependencies, as well as anything inside parameter of type annotated with @ProvidesAll or containing module.

#### Example:
    
    class Something(val name: String)

    @Create fun provideSomething(name: String): Something

Code above will be transformed to

    fun provideSomething(name: String) = Something(name)

### @CreateCached

Same as @Create, but creates a lazy property and return value from it. Functions marked with @CreateCached don't support parameters.

### @Provided

Tells compiler plugin to generate method body that returns value of specified type retrieved from dependencies. It's useful when we need to elevate dependencies from nested modules.
Doesn't call constructor.

#### Example:

    class Something(val name: String)

    class ExternalModule(
        val something: Something
    )

    @DiModule
    class MyModule(@ProvidesAll val external: ExternalModule) {
        @Provided fun provideSomething(): Something
    }

### @ProvidesByConstructor

Dependencies of types listed in this annotation parameters will be provided by constructor when required.
Might be applied to the whole module or to a single function

#### Example:

    class SomeDependency

    class Something(val dependency: SomeDependency)

    @DiModule
    @ProvidesByConstructor(SomeDependency::class)
    class MyModule {
        @Create fun provideSomething(): Something
    }

### @DiModule

Tells compiler plugin to support @Create, @CreateCached and @Provided annotations in this class and to use methods and properties in this class as dependencies. 
 
#### Example:

    class Something(val name: String)

    @DiModule
    class MyModule(val somethingName: String) {
        @Create fun provideSomething(): Something
    }

Example above will use somethingName property of MyModule to provide name parameter for Something constructor.

### @ProvidesAll

Tells compiler that dependency marked with this annotation might provide all its properties and functions as dependency.

This annotation doesn't work recursively.

#### Example:

    class ExternalModule(val name: String)

    class Something(val name: String)

    @DiModule
    class MyModule(
        @ProvidesAll
        private val external: ExternalModule
    ) {
        @Create fun provideSomething(): Something // will call constructor using external.name as parameter
    }