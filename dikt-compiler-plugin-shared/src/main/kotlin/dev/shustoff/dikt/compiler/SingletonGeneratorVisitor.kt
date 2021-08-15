package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.ModuleSingletonGenerator
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class SingletonGeneratorVisitor(
    private val errorCollector: ErrorCollector,
    private val pluginContext: IrPluginContext,
    private val singletones: MutableMap<IrType, MutableList<IrClass>>,
    private val incrementalHelper: IncrementalCompilationHelper?,
    private val singletonGenerator: ModuleSingletonGenerator,
    private val visitedModules: MutableSet<IrClass>
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (Annotations.isModule(declaration)) {
            visitedModules.add(declaration)
            val foundSingletons = singletones[declaration.defaultType].orEmpty()
            incrementalHelper?.updateSingletonsCache(declaration, foundSingletons, pluginContext)
            val allSingletons = incrementalHelper?.getCachedSingletons(declaration, pluginContext) ?: foundSingletons
            singletonGenerator.generateModuleSingletons(declaration, allSingletons)
        }
        super.visitClass(declaration)
    }
}