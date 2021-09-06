package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class WithModules(
    vararg val modules: KClass<*>
)
