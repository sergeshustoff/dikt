package dev.shustoff.dikt.sample

import dev.shustoff.dikt.test.Test

fun main() {
    Test.verify()
    println(CarModule(EngineModule(EngineNameModuleImpl("native test engine"))).getGarage())
}