package dev.shustoff.dikt.sample

import dev.shustoff.dikt.ByDi
import dev.shustoff.dikt.Module

@Module
class CarModule(
    val engineModule: EngineModule,
) {
    @ByDi fun car(): Car
}