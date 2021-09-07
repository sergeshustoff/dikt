package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Create
import dev.shustoff.dikt.CreateCached
import dev.shustoff.dikt.DiModule
import dev.shustoff.dikt.UseModules

@DiModule
@UseModules(EngineModule::class)
class CarModule(
    val engineModule: EngineModule,
) {
    @CreateCached
    fun owner(): CarOwner

    @Create fun car(): Car

    @Create fun getGarage(): Garage
}