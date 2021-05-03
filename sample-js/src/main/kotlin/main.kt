import dev.shustoff.dikt.sample.CarModule
import dev.shustoff.dikt.sample.EngineModule
import dev.shustoff.dikt.sample.EngineNameModule
import dev.shustoff.dikt.sample.getGarage
import kotlinx.browser.document

fun main() {
    val garage = CarModule(EngineModule(EngineNameModule("js test engine"))).getGarage()
    document.write(garage.toString())
}