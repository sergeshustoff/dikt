package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.IncrementalHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DiktIrGenerationExtension(
    private val errorCollector: ErrorCollector,
    private val incrementalHelper: IncrementalHelper?
) : IrGenerationExtension, ErrorCollector by errorCollector {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach {
            info("Dikt processing file: ${it.name}")
        }
        val singletones = mutableMapOf<IrType, MutableList<IrClass>>()
        moduleFragment.accept(ExternalSingletonDetector(errorCollector), singletones)
        moduleFragment.acceptVoid(ExternalSingletonCreator(errorCollector, pluginContext, singletones, incrementalHelper))
        moduleFragment.acceptVoid(ModulesVisitor(errorCollector, pluginContext, incrementalHelper))
        moduleFragment.acceptVoid(ExtensionFunctionsVisitor(errorCollector, pluginContext, incrementalHelper))

        incrementalHelper?.flush()
    }
}