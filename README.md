# DI.kt
Simple and powerful DI for kotlin multiplatform

## Installation

    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath "io.github.sergeshustoff:dikt-gradle-plugin:1.0.0-alpha1"
        }
    }
    
    apply plugin: 'io.github.sergeshustoff.dikt'
    
## Usage

Create module and declare provided dependencies. Use by factory() or by singleton() to build injected dependency.

    class CarModule(
        val externalDependency: Something,
    ): Module() {
        val injected: SomethingElse by factory()
        val singleton: SomeSingleton by singleton()
    }
  
Under the hood constructor wiht @Inject annotation will be called for SomethingElse and SomeSingleton. If there is no annotated constructor - primary construtor is used for direct dependency. If constructor requires some parameters - they will be retreived form module properties, nested modules, module functions or created by constructor with @Inject annotation (for indirect dependency annotation is required).
