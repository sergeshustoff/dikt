package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
/**
 * Tells compiler that dependencies of listed types should be created by constructor when needed.
 *
 * Primary constructor is called for creating dependencies of listed types.
 * Di functions inside module will return the new instances for listed types on each call.
 */
annotation class InjectByConstructors(
    vararg val types: KClass<*>
)