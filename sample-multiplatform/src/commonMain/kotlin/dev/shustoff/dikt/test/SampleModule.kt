package dev.shustoff.dikt.test

import dev.shustoff.dikt.*

@InjectByConstructors(SomeInjectable::class)
@InjectSingleByConstructors(Singleton::class)
class SampleModule(
    @ProvidesMembers private val nestedModule: NestedModule
) {
    fun dependencySingleton(): Dependency = resolve()
    fun injectableFactory(name: String): SomeInjectable = resolve()
    fun singleton(): Singleton = resolve()
    fun String.injectableExtension(): SomeInjectable = resolve()
}
