package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Create
import dev.shustoff.dikt.UseModules

@UseModules(EngineNameModule::class)
class EngineModule(
    private val engineNameModule: EngineNameModule<String>
) {

    @Create
    fun buildEngine(): Engine
}