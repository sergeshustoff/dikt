package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.resolveModuleDeclarations
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DiktIrGenerationExtension(
    private val errorCollector: ErrorCollector
) : IrGenerationExtension, ErrorCollector by errorCollector {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val moduleDeclarations = resolveModuleDeclarations(pluginContext)
        if (moduleDeclarations == null) {
            error("Module class not found. Make sure you initialized dikt plugin correctly")
            return
        }
        val moduleSingletones = mutableMapOf<IrType, MutableList<IrClass>>()
        moduleFragment.accept(AnnotatedSingletonDetector(), moduleSingletones)
        moduleFragment.acceptVoid(ModulesVisitor(errorCollector, moduleDeclarations, pluginContext, moduleSingletones))
    }
}