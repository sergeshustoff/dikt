import dev.shustoff.dikt.sample.CarModule
import dev.shustoff.dikt.sample.EngineModule
import kotlinx.browser.document

fun main() {
    val car = CarModule(EngineModule("js test engine")).car()
    document.write(car.toString())
}