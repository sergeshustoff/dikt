package dev.shustoff.dikt.sample

fun main() {
    println(CarModule(EngineModule("native test engine")).car())
}