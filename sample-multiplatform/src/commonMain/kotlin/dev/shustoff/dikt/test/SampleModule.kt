package dev.shustoff.dikt.test

import dev.shustoff.dikt.*

@ModuleScopes(SampleModuleScope::class)
class SampleModule(
    @ProvidesMembers private val nestedModule: NestedModule
) {
    fun dependencySingleton(): Dependency = resolve()
    fun injectableFactory(name: String): SomeInjectable = resolve()
    fun singleton(): Singleton = resolve()
    fun String.injectableExtension(): SomeInjectable = resolve()
}

object SampleModuleScope