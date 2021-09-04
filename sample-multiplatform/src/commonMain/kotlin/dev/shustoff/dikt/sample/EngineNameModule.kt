package dev.shustoff.dikt.sample

interface EngineNameModule<T> {
    val name: T
}

class EngineNameModuleImpl<T>(override val name: T) : EngineNameModule<T>