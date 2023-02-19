package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ModuleScopes(vararg val scopes: KClass<*>)
