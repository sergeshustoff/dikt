package dev.shustoff.dikt.sample

import dev.shustoff.dikt.ByDi
import dev.shustoff.dikt.DiModule
import dev.shustoff.dikt.ProvidesAllContent

@DiModule
class CarModule(
    @ProvidesAllContent val engineModule: EngineModule,
) {
    @ByDi(cached = true)
    fun owner(): CarOwner

    @ByDi fun car(): Car

    @ByDi fun getGarage(): Garage
}