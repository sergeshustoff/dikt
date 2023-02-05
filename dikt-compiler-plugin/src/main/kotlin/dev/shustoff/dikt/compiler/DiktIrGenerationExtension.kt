package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.DiNewApiCodeGenerator
import dev.shustoff.dikt.core.DiOldApiTransformer
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.recursion.RecursiveCallsDetector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DiktIrGenerationExtension(
    private val errorCollector: ErrorCollector,
    private val incrementalHelper: IncrementalCompilationHelper?
) : IrGenerationExtension, ErrorCollector by errorCollector {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(DiOldApiTransformer(errorCollector, pluginContext), null)
        moduleFragment.transform(DiNewApiCodeGenerator(errorCollector, pluginContext, incrementalHelper), DiNewApiCodeGenerator.Data())
        moduleFragment.acceptVoid(RecursiveCallsDetector(errorCollector)) //TODO: check if it still works for resolve calls inside function
    }
}