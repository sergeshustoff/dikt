package dev.shustoff.dikt.sample

import dev.shustoff.dikt.ByDi
import dev.shustoff.dikt.SingletonByDi
import dev.shustoff.dikt.Module

@Module
class CarModule(
    val engineModule: EngineModule,
) {
    @SingletonByDi fun carOwner(): CarOwner
    @ByDi fun car(): Car
}