package dev.shustoff.dikt.test

import dev.shustoff.dikt.CreateSingle

class NestedModule {
    @CreateSingle fun dependency(): Dependency
}