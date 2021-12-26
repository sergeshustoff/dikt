package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Create
import dev.shustoff.dikt.CreateSingle
import dev.shustoff.dikt.UseModules

@UseModules(EngineModule::class)
class CarModule(
    val engineModule: EngineModule,
) {
    @CreateSingle
    fun owner(): CarOwner

    @Create fun carUnknownModel(): Car

    @Create fun car(model: String): Car

    @Create fun getGarage(): Garage
}