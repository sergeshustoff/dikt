package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.DiFunctionGenerator
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
        moduleFragment.acceptVoid(DiFunctionGenerator(errorCollector, pluginContext, incrementalHelper))
        moduleFragment.acceptVoid(RecursiveCallsDetector(errorCollector))
    }
}