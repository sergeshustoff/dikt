package dev.shustoff.dikt.test

import dev.shustoff.dikt.ModuleScopes
import dev.shustoff.dikt.resolve

@ModuleScopes(NestedScope::class)
class NestedModule {
    fun dependency(): Dependency = resolve()
}

object NestedScope