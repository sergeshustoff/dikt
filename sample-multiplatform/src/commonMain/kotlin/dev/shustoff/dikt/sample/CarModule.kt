package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Module

class CarModule(
    val engineModule: EngineModule,
): Module() {
    val car: Car by factory()
}