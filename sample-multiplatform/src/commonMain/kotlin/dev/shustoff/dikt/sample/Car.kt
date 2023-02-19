package dev.shustoff.dikt.sample

import dev.shustoff.dikt.Injectable

data class Car(
    val engine: Engine,
    val model: String = defaultModel,
    val owner: CarOwner,
) : Injectable {

    companion object {
        private val defaultModel = "unknown"
    }
}