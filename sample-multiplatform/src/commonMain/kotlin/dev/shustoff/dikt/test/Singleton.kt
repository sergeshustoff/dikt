package dev.shustoff.dikt.test

import dev.shustoff.dikt.InjectableSingleInScope

class Singleton(
    private val dependency: Dependency
) : InjectableSingleInScope<SampleModuleScope>