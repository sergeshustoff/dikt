package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
/**
 * Tells compiler to create a lazy properties initialized with constructor calls for listed types and use that property in di functions.
 *
 * Primary constructor is called for creating dependencies of listed types.
 * Di functions inside module will return the same instances for listed types.
 */
annotation class InjectSingleByConstructors(
    vararg val types: KClass<*>
)
