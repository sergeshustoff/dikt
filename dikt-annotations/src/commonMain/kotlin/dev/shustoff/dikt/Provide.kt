package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("Call resolve() function instead. This annotation will be deleted in version 1.1.1")
/**
 * DI.kt compiler plugin will generate body for function with this annotation. Returned value is retrieved from available dependencies.
 *
 * It's useful for elevating dependencies from nested modules.
 * Generated code doesn't call constructor for returned type unless it's listed in @InjectByConstructors or @InjectSingleByConstructors.
 */
annotation class Provide
