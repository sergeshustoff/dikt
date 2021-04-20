package dev.shustoff.dikt.sample

import dev.shustoff.dikt.ByDi
import dev.shustoff.dikt.Cached
import dev.shustoff.dikt.Module

@Module
class CarModule(
    val engineModule: EngineModule,
) {
    @Cached
    @ByDi fun carOwner(): CarOwner
    @ByDi fun car(): Car
}