package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DiktIrGenerationExtension(
    private val errorCollector: ErrorCollector,
    private val incrementalHelper: IncrementalCompilationHelper?
) : IrGenerationExtension, ErrorCollector by errorCollector {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach {
            info("Dikt processing file: ${it.name}")
        }
        moduleFragment.acceptVoid(ModuleDiGeneratorVisitor(errorCollector, pluginContext, incrementalHelper))
    }
}