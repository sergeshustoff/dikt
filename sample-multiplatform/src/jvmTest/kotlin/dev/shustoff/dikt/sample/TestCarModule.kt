package dev.shustoff.dikt.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TestCarModule {
    private val module = CarModule(EngineModule(EngineNameModuleImpl("test engine")))

    @Test
    fun `car with model created successfully`() {
        val car = module.car("Awesome model")
        assertThat(car).isNotNull()
        assertThat(car.model).isEqualTo("Awesome model")
    }

    @Test
    fun `car created successfully`() {
        val car = module.carUnknownModel()
        assertThat(car).isNotNull()
        assertThat(car.model).isEqualTo("unknown")
    }

    @Test
    fun `cars have single owner`() {
        assertThat(module.carUnknownModel().owner).isSameInstanceAs(module.carUnknownModel().owner)
    }

    @Test
    fun `different cars are produced`() {
        assertThat(module.carUnknownModel()).isNotSameInstanceAs(module.carUnknownModel())
    }

    @Test
    fun `extension function works`() {
        val garage = module.getGarage()
        assertThat(garage).isNotNull()
    }
}