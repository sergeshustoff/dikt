[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.sergeshustoff.dikt/dikt-compiler-plugin)
[![gradle plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/io/github/sergeshustoff/dikt/dikt-gradle-plugin/maven-metadata.xml.svg?label=gradle%20plugin)](https://plugins.gradle.org/plugin/io.github.sergeshustoff.dikt)
![Kotlin version](https://kotlin-version.aws.icerock.dev/kotlin-version?group=io.github.sergeshustoff.dikt&name=dikt)

# DI.kt
**Warning**: this documentation is for library version 1.1.+ (witch is in alpha now), for older version check [old version of documentation](https://github.com/sergeshustoff/dikt/blob/3665a75221f288a1c455fe94acb2c9c84039e2af/README.md).

Simple DI with compile-time dependency graph validation for kotlin multiplatform.
It uses IR to generate code that finds or creates dependency in place of `resolve()` call.

Limitations: all annotations required for generating code should be available in the same file as generated code. It can use methods and constructors from outside, but not annotations, because adding and removing annotations in other files would not trigger recompilation for generated code and combined with incremental compilation it would cause errors.

### Why another DI?
DI.kt is smaller and simpler than some solutions, but it verifies dependency graph in compile time like a serious DI framework. 

DI.kt does not generate files during compilation, which makes compilation faster.

Because of its simplicity it might be useful for minimalistic DI in libraries and feature-modules, but it can be used in big project too.

#### Other solutions:

[Kotlin-inject](https://github.com/evant/kotlin-inject) - incredibly powerful DI framework with Dagger-like api;

[Koin](https://github.com/InsertKoinIO/koin), [Kodein-DI](https://github.com/Kodein-Framework/Kodein-DI) and [PopKorn](https://github.com/corbella83/PopKorn) - service locators with great versatility, but without compile time error detection that we used to have in Dagger;

[Dagger](https://github.com/google/dagger) - most popular DI framework for Android, but it doesn't support multiplatform yet.

### Sample
I forked kotlin multiplatform sample [here](https://github.com/sergeshustoff/PeopleInSpace-dikt-sample) and replaced di with DI.kt. It's clumsy, but it shows that library works on different platforms.

[Here](https://github.com/holdbetter/PremierLeague) you can find more extensive project by [@holdbetter](https://github.com/holdbetter) that uses DI.kt.

## Installation

#### Compatibility with kotlin versions:

Because library uses undocumented compiler api that often changes each library version works well only with specific kotlin versions, check table bellow to decide witch library version to use.

| DI.kt version       | Supported kotlin versions         |
|---------------------|-----------------------------------|
| 1.1.0-kotlin-2.0.20 | 2.0.20 - 2.1.0 (k2 with new api)  |
| 1.1.0-kotlin-2.0.0  | 2.0.0 - 2.0.10 (k2 with new api)  |
| 1.1.0               | 1.8.20 - 1.9.20 (k2 with new api) |
| 1.1.0-kotlin-1.8.10 | 1.8.0 - 1.8.10 (k2 with new api)  |
| 1.1.0-kotlin-1.7.21 | 1.7.20 - 1.7.21 (k2 with new api) |
| 1.0.4               | 1.8.20 - 1.8.21 (k2 with hacks)   |
| 1.0.3               | 1.8.0 - 1.8.10 (k2 with hacks)    |
| 1.0.2               | 1.7.0 - 1.7.21                    |
| 1.0.1               | 1.6.2x                            |
| 1.0.0-alpha9        | 1.6.10                            |
| 1.0.0-alpha7        | 1.6.0                             |

#### K2 support:

Version 1.0.3 has limited support for k2, but requires hack to work properly - annotation @Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY") is required for module or each di function because plugin can't bypass function body check on k2 for now.

Version 1.1.0 has new api compatible with k2 without additional hacks. Old incompatible api is deprecated

#### Gradle plugin:
In build.gradle file in module add plugin:

```groovy
plugins {
    id 'io.github.sergeshustoff.dikt' version '1.1.0'
}
```

Or use this if you only need base api without code generation (Injectable and InjectableSingleInScope interfaces):

```groovy
dependencies {
    implementation "io.github.sergeshustoff.dikt:dikt:1.1.0"
}
```

## Usage

Create module and declare provided dependencies. 
Use `resolve()` function in places where you need your dependencies provided/created.
Use `@ProvidesMembers`, `@InjectByConstructors` and `@InjectSingleByConstructors` annotations, or `Injectable` and `InjectableSingleInScope<Scope>` interfaces to control how dependencies are provided and what classes can be created by primary constructors.

#### Example:
```kotlin
@InjectSingleByConstructors(SomeSingleton::class)
@InjectByConstructors(SomethingElse::class)
class SomethingModule(
    val externalDependency: Something,
) {
    fun someSingleton(): SomeSingleton = resolve()
    fun provideSomethingElse(): SomethingElse = resolve()
}
```

#### Generated code:
```kotlin
@InjectSingleByConstructors(SomeSingleton::class)
@InjectByConstructors(SomethingElse::class)
class SomethingModule(
    val externalDependency: Something,
) {
    private val someSingletonBackingField by lazy { SomeSingleton(externalDependency) }
    fun someSingleton(): SomeSingleton = someSingletonBackingField
    fun provideSomethingElse(): SomethingElse = SomethingElse(someSingletonBackingField)
}
```
  
Under the hood primary constructor will be called for SomethingElse and SomeSingleton. If constructor requires some parameters - they will be retrieved form module's properties and functions, and from function parameters.

### `resolve()` function limitations
`resolve()` function can be called only in functions bodies. It shouldn't be called in constructors or property initializers


#### Example

```kotlin
class Module {
    fun something(): Something = resolve() // correct
    
    fun processSomething() {
        val something: Something = resolve() // correct
    }
    
    val something: Something = resolve() // incorrect
    val something: Something by lazy { resolve() } // incorrect
    val something: Something get() = resolve() // incorrect
    init {
        val something: Something = resolve() // incorrect
    }
}
```

### Recursive dependency
Library will fail compilation if there are any recursive dependencies in generated code, but there might be a situation when users code uses generated code to provide dependencies for injection.
In this case it's better to write a function and list all needed dependencies in parameters instead of calling module functions directly:

#### Don't do this:

```kotlin
class AutoInjectable(val manual: ManualInjectable)

class ManualInjectable(val injectable: AutoInjectable)

@InjectByConstructors(AutoInjectable::class)
class MyModule {
    fun injectable(): AutoInjectable = resolve()
    
    fun manualInjectable() = ManualInjectable(injectable()) // here is the recursion in users code that will not be detected by library
}
```

#### Do this instead:

```kotlin
class AutoInjectable(val manual: ManualInjectable)

class ManualInjectable(val injectable: AutoInjectable)

@InjectByConstructors(AutoInjectable::class)
class MyModule {
    fun injectable(): AutoInjectable = resolve()

    fun manualInjectable(injectable: AutoInjectable) = ManualInjectable(injectable) // this way library detects recursion and fails compilation
}
```

### Module
Any class or object that has `resolve()` calls somewhere inside is essentially a module. We don't need additional annotation for it, but if you need content of another 'module' provided as dependency in generated code, you need to mark a function or property returning that class as module using annotation `@ProvidesMembers`.

If you need module to contain singletons of some scope mark it with `@ModuleScopes(YourScope::class)`

### Singleton
There are no true singletons in DI.kt, but instead you can use `@InjectSingleByConstructors`. When resolving types listed in this annotation backing lazy field will be generated in module and instance of that type will be reused when it's resolved again. Such singleton instances persist as long as they resolved for the same instance of containing class (module). 
Effectively it gives each module a scope of their own and makes the scoping more understandable.

Same effect as with `@InjectSingleByConstructors` can be achieved using `InjectableSingleInScope<Scope>` interface on dependency and `@ModuleScopes(Scope::class)` annotation on module.

### `Injectable` interface

Dependencies directly implementing interface `Injectable` will be provided by constructor when required. It is similar to @Inject annotation in java, but for compiler plugin compatibility with incremental compilation it had to be an interface (changing annotations will not cause dependant code to recompile, while changing supertypes will).

This interface can be used in project without dikt compiler plugin:

```groovy
dependencies {
    implementation 'io.github.sergeshustoff.dikt:dikt:1.1.0'
}
```

#### Example:

```kotlin
class SomeDependency : Injectable

class Something(val dependency: SomeDependency) : Injectable

class MyModule {
    fun provideSomething(): Something = resolve()
}
```

#### Generated code:

```kotlin
class SomeDependency : Injectable

class Something(val dependency: SomeDependency) : Injectable

class MyModule {
    fun provideSomething(): Something = Something(SomeDependency())
}
```

### `InjectableSingleInScope<Scope>` interface

Similar to `Injectable` interface, but for types implementing this interface directly can only be created in module annotated with @ModuleScopes(Scope::class) with the same type used for scope.

Only one instance of that type will be created in module, lazy backing field will be used to ensure that.

This interface can be used in project without dikt compiler plugin, same as `Injectable`:

#### Example:

```kotlin
object Scope

class SomeSingleton : InjectableSingleInScope<Scope>

class Something(val dependency: SomeSingleton) : Injectable

@ModuleScopes(Scope::class)
class MyModule {
    fun provideSomething(): Something = resolve()
}
```

#### Generated code:

```kotlin
object Scope

class SomeSingleton : InjectableSingleInScope<Scope>

class Something(val dependency: SomeSingleton) : Injectable

class MyModule {
    private val _someSingleton by lazy { SomeSingleton() }
    fun provideSomething(): Something = Something(_someSingleton)
}
```

## Annotations

For controlling how things created or provided by di there are a few annotations. Those annotations allow to use DI.kt without injectable code knowing anything about DI.kt.

### `@InjectByConstructors`

Dependencies of types listed in this annotation parameters will be provided by constructor when required.

Might be applied to file, class or function.

#### Example:

```kotlin
class SomeDependency

class Something(val dependency: SomeDependency)

@InjectByConstructors(Something::class, SomeDependency::class)
class MyModule {
    fun provideSomething(): Something = resolve()
}
```

#### Generated code:

```kotlin
class SomeDependency

class Something(val dependency: SomeDependency)

class MyModule {
    fun provideSomething(): Something = Something(SomeDependency())
}
```

### `@InjectSingleByConstructors`

Dependencies of types listed in this annotation parameters will be provided by constructor when required, plus instance will be cached for future use in the same module by backing lazy field.

Might be applied to class.

#### Example:

```kotlin
class SomeDependency

class Something(val dependency: SomeDependency)

@InjectByConstructors(Something::class)
@InjectSingleByConstructors(SomeDependency::class)
class MyModule {
    fun provideSomething(): Something = resolve()
}
```

#### Generated code:

```kotlin
class SomeDependency

class Something(val dependency: SomeDependency)

class MyModule {
    private val cachedDependency by lazy { SomeDependency() }
    fun provideSomething(): Something = Something(cachedDependency)
}
```

### `@ProvidesMembers`

Indicates that all visible members of dependency returned from marked function or property can be used in dependency resolution.

If dependency is available in a nested module it will be used from there instead of calling a constructor even if it's marked with `@InjectByConstructors` or implements `Injectable`.

WARNING: This annotation doesn't work recursively. It means that any `@ProvidesMembers` annotations in other classes or files will be ignored when generation dependency resolution.

#### Example:

```kotlin
class ExternalModule(val name: String)

class Something(val name: String)

@InjectByConstructors(Something::class)
class MyModule(
    @ProvidesMembers private val external: ExternalModule
) {
    fun provideSomething(): Something = resolve()
}
```

#### Generated code:

```kotlin
class ExternalModule(val name: String)

class Something(val name: String)

class MyModule(
    private val external: ExternalModule
) {
    fun provideSomething(): Something = Something(external.name)
}
```

### `@ModuleScopes`

Indicates that singletons for given scopes can be created in module marked with the annotation. Allows multiple scopes for a single module if needed.

Should be used in pair with InjectableSingleInScope interface on dependencies, scope should be the same in @ModuleScopes and in type parameter of InjectableSingleInScope

#### Example:

```kotlin
object Scope

class SomeSingleton : InjectableSingleInScope<Scope>

class Something(val dependency: SomeSingleton) : Injectable

@ModuleScopes(Scope::class)
class MyModule {
    fun provideSomething(): Something = resolve()
}
```

#### Generated code:

```kotlin
object Scope

class SomeSingleton : InjectableSingleInScope<Scope>

class Something(val dependency: SomeSingleton) : Injectable

class MyModule {
    private val _someSingleton by lazy { SomeSingleton() }
    fun provideSomething(): Something = Something(_someSingleton)
}
```