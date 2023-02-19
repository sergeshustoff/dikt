package dev.shustoff.dikt

/**
 * When class directly implements this interface compiler plugin will be able to create instances of this class when replacing `resolve()` calls in modules marked with `@ModuleScopes(Scope::class)` as long as scope in module matches scope in InjectableSingleInScope generic parameter.
 * This removes requirement for listing singleton types in module
 */
interface InjectableSingleInScope<Scope: Any>