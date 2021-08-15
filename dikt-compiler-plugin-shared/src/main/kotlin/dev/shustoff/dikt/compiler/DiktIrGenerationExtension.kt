package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.ModuleSingletonGenerator
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
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
    private val incrementalHelper: IncrementalCompilationHelper?
) : IrGenerationExtension, ErrorCollector by errorCollector {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach {
            info("Dikt processing file: ${it.name}")
        }
        val singletonGenerator = ModuleSingletonGenerator(pluginContext, errorCollector, incrementalHelper)
        val singletones = mutableMapOf<IrType, MutableList<IrClass>>()
        val visitedModules = mutableSetOf<IrClass>()
        moduleFragment.accept(ExternalSingletonDetector(errorCollector), singletones)
        moduleFragment.acceptVoid(SingletonGeneratorVisitor(errorCollector, pluginContext, singletones, incrementalHelper, singletonGenerator, visitedModules))
        moduleFragment.acceptVoid(ModulesVisitor(errorCollector, pluginContext, incrementalHelper, singletonGenerator))
        moduleFragment.acceptVoid(ExtensionFunctionsVisitor(errorCollector, pluginContext, incrementalHelper, singletonGenerator))

        //TODO: don't call on full compilation
        incrementalHelper?.markChangedDependenciesForRecompilation(pluginContext, visitedModules)

        incrementalHelper?.flush()
    }
}