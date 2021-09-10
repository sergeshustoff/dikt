package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
/**
 * Same as @Provide, but tells compiler to create a lazy property in containing class and return value from that property.
 *
 * Functions marked with @ProvideSingle don't support parameters.
 */
annotation class ProvideSingle
