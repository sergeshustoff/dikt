package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Module
import dev.shustoff.dikt.Named

@Module
class EngineNameModule<T>(
    @Named("engineName") val name: T
)