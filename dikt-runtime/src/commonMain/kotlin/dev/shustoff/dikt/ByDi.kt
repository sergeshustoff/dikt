package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ByDi(
    val cached: Boolean = false
)
