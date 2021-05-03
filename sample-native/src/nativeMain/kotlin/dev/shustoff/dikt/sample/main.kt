package dev.shustoff.dikt.sample

fun main() {
    println(CarModule(EngineModule(EngineNameModule("native test engine"))).getGarage())
}