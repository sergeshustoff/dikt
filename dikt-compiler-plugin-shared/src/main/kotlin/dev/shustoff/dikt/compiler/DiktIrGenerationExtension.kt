package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DiktIrGenerationExtension(
    private val errorCollector: ErrorCollector
) : IrGenerationExtension, ErrorCollector by errorCollector {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val singletones = mutableMapOf<IrType, MutableList<IrClass>>()
        moduleFragment.accept(ExternalSingletonDetector(errorCollector), singletones)
        moduleFragment.acceptVoid(ExternalSingletonCreator(errorCollector, pluginContext, singletones))
        singletones.values.flatten().forEach { singleton ->
            singleton.error("Module not found")
        }

        moduleFragment.acceptVoid(ModulesVisitor(errorCollector, pluginContext))
        moduleFragment.acceptVoid(ExtensionFunctionsVisitor(errorCollector, pluginContext))
    }
}