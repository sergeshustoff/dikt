package dev.shustoff.dikt.core

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny
import org.jetbrains.kotlin.name.FqName

class ModuleDeclarations(
    private val moduleClass: IrClassSymbol,
    private val diFunctions: DiFunctions
) {
    private val diFunctionsSet = setOf(diFunctions.factory, diFunctions.singleton)

    fun isModule(declaration: IrClass) = declaration.superTypes.any { it.classOrNull == moduleClass }
    fun isDiFunction(function: IrSimpleFunction): Boolean {
        return function.isFakeOverride &&
                !function.isFakeOverriddenFromAny() &&
                function.realOverrideTarget.symbol in diFunctionsSet
    }

    fun isSingleton(function: IrSimpleFunction): Boolean {
        return function.isFakeOverride &&
                !function.isFakeOverriddenFromAny() &&
                function.realOverrideTarget.symbol == diFunctions.singleton
    }
}

fun resolveModuleDeclarations(pluginContext: IrPluginContext): ModuleDeclarations? {
    val diFunctions = buildDiFunctions(pluginContext)
    val moduleClass = pluginContext.referenceClass(FqName("dev.shustoff.dikt.Module"))

    return if (moduleClass != null && diFunctions != null) {
        ModuleDeclarations(moduleClass, diFunctions)
    } else {
        null
    }
}