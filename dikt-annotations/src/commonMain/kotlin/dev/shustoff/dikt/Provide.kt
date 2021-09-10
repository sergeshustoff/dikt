package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
/**
 * Tells compiler plugin to generate method body that returns value retrieved from dependencies. For example from containing class properties or functions.
 *
 * It's useful for elevating dependencies from nested modules.
 * Generated code doesn't call constructor for returned type unless it's listed in @UseConstructors.
 */
annotation class Provide
