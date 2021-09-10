package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
/**
 * Dependencies of types listed in this annotation parameters will be provided by constructor when required.
 * Might be applied to file, class, or @Create, @Provide, @CreateSingle and @ProvideSingle function.
 *
 * When constructor called for returned type of @Create function requires parameter of type listed in @UseConstructors it's constructor will be called instead of looking for provided dependency of that type.
 */
annotation class UseConstructors(
    vararg val types: KClass<*>
)
