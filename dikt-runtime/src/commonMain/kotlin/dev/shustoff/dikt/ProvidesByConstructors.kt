package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ProvidesByConstructor(
    vararg val types: KClass<*>
)
