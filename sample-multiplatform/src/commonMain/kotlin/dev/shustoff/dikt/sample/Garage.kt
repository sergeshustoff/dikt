package dev.shustoff.dikt.sample

import dev.shustoff.dikt.ByDi

data class Garage(
    val car: Car
)

@ByDi fun CarModule.getGarage(): Garage