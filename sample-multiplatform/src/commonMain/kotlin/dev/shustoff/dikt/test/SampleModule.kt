package dev.shustoff.dikt.test

import dev.shustoff.dikt.*

@UseModules(NestedModule::class)
class SampleModule(
    val nestedModule: NestedModule
) {
    @ProvideSingle fun dependencySingleton(): Dependency
    @Create fun injectableFactory(name: String): Injectable
    @CreateSingle fun singleton(): Singleton
    @Create fun String.injectableExtension(): Injectable
}

@UseModules(SampleModule::class, NestedModule::class)
@Create fun SampleModule.externalExtension(name: String): Injectable