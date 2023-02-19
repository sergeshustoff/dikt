package dev.shustoff.dikt

/**
 * DI.kt compiler plugin will generate expression instead of this function call.
 * Returned value is retrieved from available dependencies (parent class properties and functions, parameters of a function where it's called).
 *
 * Generated code doesn't call constructor for returned type unless it's listed in @InjectByConstructors or @InjectSingleByConstructors.
 */
fun <T> resolve(): T = throw Exception("Incorrect use of DI.kt. Did you call this function outside of kotlin code?")