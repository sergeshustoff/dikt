package dev.shustoff.dikt.sample

fun main() {
    println(CarModule(EngineModule(EngineNameModuleImpl("native test engine"))).getGarage())
}