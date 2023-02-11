package dev.shustoff.dikt

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
/**
 * Marks function or property as returning "Module". All visible properties and functions of returned type can be used for resolving dependencies.
 *
 * This annotation doesn't work recursively. It means that "resolve()" can only use members of dependencies annotated with ProvidesMembers in containing class, any ProvidesMembers annotations in other classes or files will be ignored.
 */
annotation class ProvidesMembers()
