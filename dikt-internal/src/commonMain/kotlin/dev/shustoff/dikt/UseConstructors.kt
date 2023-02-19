package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("Use InjectByConstructors or InjectSingleByConstructors instead. This annotation will be deleted in version 1.1.1")
/**
 * Dependencies of types listed in this annotation parameters will be provided by constructor when required in generated function body.
 * Annotation might be applied to file, class, or @Create, @Provide and @CreateSingle function.
 *
 * When constructor called for returned type of @Create function requires parameter of type listed in @UseConstructors it's constructor will be called instead of looking for provided dependency of that type.
 */
annotation class UseConstructors(
    vararg val types: KClass<*>
)
