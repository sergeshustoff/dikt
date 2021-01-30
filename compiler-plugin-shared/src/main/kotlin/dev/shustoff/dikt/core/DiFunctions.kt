package dev.shustoff.dikt.core

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.FqName

class DiFunctions(
    val factory: IrSimpleFunctionSymbol,
    val singleton: IrSimpleFunctionSymbol
)

fun buildDiFunctions(pluginContext: IrPluginContext): DiFunctions? {
    val factory = pluginContext.referenceFunctions(FqName("dev.shustoff.dikt.Module.factory"))
        .firstOrNull() ?: return null
    val singleton = pluginContext.referenceFunctions(FqName("dev.shustoff.dikt.Module.singletone"))
        .firstOrNull() ?: return null
    return DiFunctions(factory, singleton)
}