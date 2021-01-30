package dev.shustoff.dikt

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Module {

    protected fun <T> factory(creator: (() -> T)? = null): ReadOnlyProperty<Module, T> =
        FactoryDelegate<T>(creator)

    protected fun <T> singletone(creator: (() -> T)? = null) =
        lazy(
            creator ?: throw IllegalStateException(
                "Module ${this::class.simpleName} wasn't property initialized, check if dikt plugin is set up"
            )
        )

    private class FactoryDelegate<T>(
        private val creator: (() -> T)?
    ) : ReadOnlyProperty<Module, T> {

        override fun getValue(thisRef: Module, property: KProperty<*>): T {
            val creator = creator
                ?: throw IllegalStateException("Module ${thisRef::class.simpleName} wasn't property initialized, check if dikt plugin is set up")
            return creator.invoke()
        }
    }
}
