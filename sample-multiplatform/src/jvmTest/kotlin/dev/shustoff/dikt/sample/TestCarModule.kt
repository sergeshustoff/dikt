package dev.shustoff.dikt.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TestCarModule {
    private val module = CarModule(EngineModule(EngineNameModule("test engine")))
    @Test
    fun `car created successfully`() {
        assertThat(module.car()).isNotNull()
    }

    @Test
    fun `cars have single owner`() {
        assertThat(module.car().owner).isSameInstanceAs(module.car().owner)
    }

    @Test
    fun `different cars are produced`() {
        assertThat(module.car()).isNotSameInstanceAs(module.car())
    }

    @Test
    fun `extension function works`() {
        val garage = module.getGarage()
        assertThat(garage).isNotNull()
    }
}