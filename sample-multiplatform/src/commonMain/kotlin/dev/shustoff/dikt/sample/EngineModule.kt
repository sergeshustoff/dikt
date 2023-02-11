package dev.shustoff.dikt.sample

import dev.shustoff.dikt.*

@InjectByConstructors(Engine::class)
class EngineModule(
    @ProvidesMembers private val engineNameModule: EngineNameModule<String>
) {

    fun buildEngine(): Engine = resolve()
}