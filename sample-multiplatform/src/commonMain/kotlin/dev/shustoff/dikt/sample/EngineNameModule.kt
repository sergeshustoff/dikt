package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Module
import dev.shustoff.dikt.Named

@Module
interface EngineNameModule<T> {
    @Named("engineName")
    val name: T
}

class EngineNameModuleImpl<T>(override val name: T) : EngineNameModule<T>