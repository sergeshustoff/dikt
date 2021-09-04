package dev.shustoff.dikt.sample

import dev.shustoff.dikt.ByDi
import dev.shustoff.dikt.DiModule
import dev.shustoff.dikt.ProvidesAllContent

@DiModule
class EngineModule(
    @ProvidesAllContent
    private val engineNameModule: EngineNameModule<String>
) {

    @ByDi
    fun buildEngine(): Engine
}