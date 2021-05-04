import dev.shustoff.dikt.sample.*
import kotlinx.browser.document

fun main() {
    val garage = CarModule(EngineModule(EngineNameModuleImpl("js test engine"))).getGarage()
    document.write(garage.toString())
}