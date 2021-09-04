package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Create
import dev.shustoff.dikt.DiModule
import dev.shustoff.dikt.ProvidesAll

@DiModule
class EngineModule(
    @ProvidesAll
    private val engineNameModule: EngineNameModule<String>
) {

    @Create
    fun buildEngine(): Engine
}