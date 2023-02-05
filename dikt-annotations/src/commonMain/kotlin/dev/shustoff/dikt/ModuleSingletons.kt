package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
/**
 * Tells compiler to create a lazy properties for listed types and use that property in di functions.
 *
 * Di functions inside module will return the same instances for listed types.
 */
annotation class ModuleSingletons(
    vararg val types: KClass<*>
)
