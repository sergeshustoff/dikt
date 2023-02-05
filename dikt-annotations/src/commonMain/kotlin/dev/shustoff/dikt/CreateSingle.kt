package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("Call resolve() function instead and add @InjectSingleByConstructors annotation with list of types that can be created by constructor and should be singletons inside given module. This annotation will be deleted in version 1.1.1")
/**
 * Same as @Create, but tells compiler to create a lazy property in containing class and return value from that property.
 *
 * Functions marked with @CreateSingle don't support parameters.
 */
annotation class CreateSingle
