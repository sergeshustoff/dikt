package dev.shustoff.dikt.test

object Test {
    fun verify() {
        val nestedModule = NestedModule()
        val module = SampleModule(nestedModule)
        check(nestedModule.dependency() !== nestedModule.dependency()) {
            "Should create new instence by @Create"
        }
        check(module.dependencySingleton() === module.dependencySingleton()) {
            "Should use single instance by @ProvideCached"
        }
        check(module.singleton() === module.singleton()) {
            "Should use single instance by @CreateCached"
        }
        check(module.injectableFactory("asd") != null) {
            "Should return value from generated function"
        }
        check(with(module) { "test".injectableExtension() } != null) {
            "Should return value from generated function"
        }
        check(module.externalExtension("test") != null) {
            "Should return value from generated function"
        }
    }
}