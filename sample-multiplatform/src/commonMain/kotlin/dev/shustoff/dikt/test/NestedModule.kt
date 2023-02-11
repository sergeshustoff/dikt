package dev.shustoff.dikt.test

import dev.shustoff.dikt.InjectSingleByConstructors
import dev.shustoff.dikt.resolve

@InjectSingleByConstructors(Dependency::class)
class NestedModule {
    fun dependency(): Dependency = resolve()
}