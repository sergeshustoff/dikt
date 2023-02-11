package dev.shustoff.dikt.test

import dev.shustoff.dikt.*

@InjectByConstructors(Injectable::class)
@InjectSingleByConstructors(Singleton::class)
class SampleModule(
    @ProvidesMembers private val nestedModule: NestedModule
) {
    fun dependencySingleton(): Dependency = resolve()
    fun injectableFactory(name: String): Injectable = resolve()
    fun singleton(): Singleton = resolve()
    fun String.injectableExtension(): Injectable = resolve()
}
