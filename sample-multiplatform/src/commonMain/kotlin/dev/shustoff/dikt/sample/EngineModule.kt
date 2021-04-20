package dev.shustoff.dikt.sample

import dev.shustoff.dikt.InjectNamed
import dev.shustoff.dikt.Module
import dev.shustoff.dikt.Named

@Module
class EngineModule(
    @Named("engineName")
    val engineName: String
) {

    fun buildEngine(@InjectNamed("engineName") name: String): Engine {
        return Engine(name)
    }
}