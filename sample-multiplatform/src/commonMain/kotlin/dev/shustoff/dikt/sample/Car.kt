package dev.shustoff.dikt.sample

data class Car(
    val engine: Engine,
    val model: String = defaultModel,
    val owner: CarOwner,
) {

    companion object {
        private val defaultModel = "unknown"
    }
}