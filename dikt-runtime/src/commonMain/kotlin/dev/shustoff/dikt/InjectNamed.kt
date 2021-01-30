package dev.shustoff.dikt

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class InjectNamed(val name: String)
