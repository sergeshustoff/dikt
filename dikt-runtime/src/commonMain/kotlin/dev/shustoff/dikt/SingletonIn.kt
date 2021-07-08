package dev.shustoff.dikt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class SingletonIn(val clazz: KClass<*>)
