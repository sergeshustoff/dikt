package dev.shustoff.dikt.test

object Test {
    fun verify() {
        val nestedModule = NestedModule()
        val module = SampleModule(nestedModule)
        check(nestedModule.dependency() === nestedModule.dependency()) {
            "Should use single instance of Dependency"
        }
        check(module.dependencySingleton() === module.dependencySingleton()) {
            "Should use single instance of provided Dependency"
        }
        check(module.singleton() === module.singleton()) {
            "Should use single instance of Singleton"
        }
        check(module.injectableFactory("asd") != null) {
            "Should return value from generated function"
        }
        check(with(module) { "test".injectableExtension() } != null) {
            "Should return value from generated function"
        }
    }
}