import dev.shustoff.dikt.sample.*
import dev.shustoff.dikt.test.Test
import kotlinx.browser.document

fun main() {
    Test.verify()
    val garage = CarModule(EngineModule(EngineNameModuleImpl("js test engine"))).getGarage()
    document.write(garage.toString())
}