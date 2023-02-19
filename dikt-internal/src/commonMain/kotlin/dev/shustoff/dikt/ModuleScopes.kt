package dev.shustoff.dikt

import kotlin.reflect.KClass

/**
 * Binds module to a given scope. Can be used in pair with InjectableSingleInScope interface on dependencies and indicates that singletons of a given scope can be created in module.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ModuleScopes(vararg val scopes: KClass<*>)
