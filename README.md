[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin)
[![gradle plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/io/github/sergeshustoff/dikt/dikt-gradle-plugin/maven-metadata.xml.svg?label=gradle%20plugin)](https://plugins.gradle.org/plugin/io.github.sergeshustoff.dikt)
[![IDEA plugin](https://img.shields.io/jetbrains/plugin/v/17533-di-kt.svg)](https://plugins.jetbrains.com/plugin/17533-di-kt)
![Kotlin version](https://kotlin-version.aws.icerock.dev/kotlin-version?group=io.github.sergeshustoff.dikt&name=dikt-compiler-plugin)

# DI.kt
Simple DI with compile-time dependency graph validation for kotlin multiplatform.
It uses IR to create method's bodies with dependency injection.

Limitations: all annotations required for generating functions should be available in the same file as generated function. It can use methods and constructors from outside, but not annotations, because adding and removing annotations in other files would not trigger recompilation for generated function and combined with incremental compilation it would cause errors.

### Why another DI?
DI.kt is smaller and simpler than some solutions, but it verifies dependency graph in compile time like a serious DI framework. 

DI.kt does not generate files during compilation, which makes compilation faster (presumably, not tested).

Because of its simplicity it might be useful for minimalistic DI in libraries and feature-modules, but it can be used in big project too.

#### Other solutions:

[Kotlin-inject](https://github.com/evant/kotlin-inject) - incredibly powerful DI framework with Dagger-like api;

[Koin](https://github.com/InsertKoinIO/koin), [Kodein-DI](https://github.com/Kodein-Framework/Kodein-DI) and [PopKorn](https://github.com/corbella83/PopKorn) - service locators with great versatility, but without compile time error detection that we used to have in Dagger;

[Dagger](https://github.com/google/dagger) - most popular DI framework for Android, but it doesn't support multiplatform yet.

### Sample
I forked kotlin multiplatform sample [here](https://github.com/sergeshustoff/PeopleInSpace-dikt-sample) and replaced di with DI.kt. It's clumsy, but it shows that library works on different platforms. 

### Articles
[DI.kt, One of the First Kotlin Multiplatform DI Libraries](https://medium.com/wriketechclub/di-kt-one-of-the-first-kotlin-multiplatform-di-libraries-5a5fd8665713)

## Installation

#### Compatibility with kotlin versions:

Because library uses undocumented compiler api that often changes each library version works well only with specific kotlin versions, check table bellow to decide witch library version to use.

| DI.kt version | Supported kotlin versions |
|---------------|---------------------------|
| 1.0.3-alpha1  | 1.8.0 (no k2)             |
| 1.0.2         | 1.7.0 - 1.7.21            |
| 1.0.1         | 1.6.2x                    |
| 1.0.0-alpha9  | 1.6.10                    |
| 1.0.0-alpha7  | 1.6.0                     |

#### Gradle plugin:
In build.gradle file in module add plugin:

```groovy
plugins {
    ...
    id 'io.github.sergeshustoff.dikt' version '1.0.2'
}
```

#### IDEA plugin

Install [idea plugin](https://plugins.jetbrains.com/plugin/17533-di-kt), it will remove errors from ide for methods with generated body.

## Usage

Create module and declare provided dependencies. Use `@Create`, `@Provide`, `@CreateSingle` and `@ProvideSingle` to generate function's bodies. Use `@UseModule`s and `@UseConstructors` to control how dependencies are provided and what classes can be created by primary constructors.

```kotlin
class SomethingModule(
    val externalDependency: Something,
) {
    @CreateSingle fun someSingleton(): SomeSingleton
    @Create fun provideSomethingElse(): SomethingElse
}
```
  
Under the hood primary constructor will be called for SomethingElse and SomeSingleton. If constructor requires some parameters - they will be retrieved form module's properties and functions.

### Module
Any class or object that has a function marked with `@Create`, `@Provide`, `@CreateSingle` or `@ProvideSingle` is essentially a module. We don't need additional annotation for it, but if you need content of another 'module' provided as dependency in generated functions, you need to mark that type as module using annotation `@UseModules` on function, its containing class or file.

### Singleton
There are no true singletons in DI.kt, but instead you can use `@CreateSingle` or `@ProvideSingle` annotations to generate functions backed by lazy properties. Such function will return the same instance each time they called as long as they called for the same instance of containing class. Effectively it gives each module a scope of their own and makes the scoping more understandable.

## Annotations

### `@Create`

Magical annotation that tells compiler plugin to generate method body using returned type's primary constructor.
Values for constructor parameters will be retrieved from function parameters and from functions and properties of containing class.

Code generated by this annotation always uses returned type's primary constructor, even if dependency of returned type is available in parameters or in containing class.

#### Example:
    
```kotlin
class Something(val name: String)

@Create fun provideSomething(name: String): Something
```

Code above will be transformed into

```kotlin
fun provideSomething(name: String) = Something(name)
```

### `@Provide`

Tells compiler plugin to generate method body that returns value of specified type retrieved from dependencies. For example from containing class properties or functions. 

It's useful for elevating dependencies from nested modules.
Doesn't call constructor for returned type unless it's listed in `@UseConstructors`.

#### Example:

```kotlin
class Something(val name: String)

class ExternalModule(
    val something: Something
)

@UseModules(ExternalModule::class)
class MyModule(val external: ExternalModule) {
    @Provide fun provideSomething(): Something
}
```

### `@CreateSingle` and `@ProvideSingle`

Same as `@Create` and `@Provide`, but each annotation tells compiler to create a lazy property in containing class and return value from that property. Functions marked with `@CreateSingle` and `@ProvideSingle` don't support parameters.

### `@UseConstructors`

Dependencies of types listed in this annotation parameters will be provided by constructor when required.

Might be applied to file, class, or `@Create` or `@Provide` function.

When constructor called for returned type of `@Create` function requires parameter of type listed in `@UseConstructors` it's constructor will be called instead of looking for provided dependency of that type.

#### Example:

```kotlin
class SomeDependency

class Something(val dependency: SomeDependency)

@UseConstructors(SomeDependency::class)
class MyModule {
    @Create fun provideSomething(): Something
}
```

### `@UseModules`

Marks types that should provide all visible properties and functions as dependencies. Such dependencies can be used in `@Create` function as constructor parameters or in `@Provide` function as returned type.

Listed type should be available from DI function in order to provide type's properties and functions.

WARNING: This annotation doesn't work recursively. It means that function can only use modules listed in its own annotation or in its class annotation or in its file annotation. 

#### Example:

```kotlin
class ExternalModule(val name: String)

class Something(val name: String)

@UseModules(ExternalModule::class)
class MyModule(
    private val external: ExternalModule
) {
    @Create fun provideSomething(): Something // will call constructor using external.name as parameter
}
```
