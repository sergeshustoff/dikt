package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
/**
 * Marks types that should provide all visible properties and functions as dependencies. Such dependencies can be used in @Create function as constructor parameters or in @Provide function as returned type.
 * Listed type should be available from DI function in order to provide type's properties and functions.
 *
 * This annotation doesn't work recursively. It means that function can only use modules listed in its own annotation or in its class annotation or in its file annotation.
 */
@Deprecated("Use ProvidesMembers instead. It's a bit more strict, but much more readable")
annotation class UseModules(
    vararg val modules: KClass<*>
)
