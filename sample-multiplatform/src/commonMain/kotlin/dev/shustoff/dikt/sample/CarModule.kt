package dev.shustoff.dikt.sample

import dev.shustoff.dikt.*

@InjectSingleByConstructors(CarOwner::class)
@InjectByConstructors(Car::class, Garage::class)
class CarModule(
    @ProvidesMembers private val engineModule: EngineModule,
) {
    fun owner(): CarOwner = resolve()

    fun carUnknownModel(): Car = resolve()

    fun car(model: String): Car = resolve()

    fun getGarage(): Garage = resolve()
}