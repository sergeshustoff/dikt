package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Inject

@Inject
data class Car(
    val engine: Engine,
    val owner: CarOwner
)