package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.DependencyCollector
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.Utils
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class DiFunctionGenerator(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    private val providedByConstructorInClassCache = mutableMapOf<IrClass, List<IrType>>()
    private val providedByConstructorInFileCache = mutableMapOf<IrFile, List<IrType>>()

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(module: IrClass) {
        if (Annotations.isModule(module)) {
            if (module.isInterface) {
                module.error("Interface modules not supported")
            } else if (!module.isFinalClass) {
                module.error("Module should be final")
            } else {
                module.functions.filter { Annotations.isByDi(it) }
                    .toList()
                    .forEach { function ->
                        buildFunctionBody(module, function)
                    }
            }
        }
        super.visitClass(module)
    }

    private fun buildFunctionBody(
        module: IrClass,
        function: IrFunction
    ) {
        //TODO: cache something for module
        val dependencies = dependencyCollector.collectDependencies(module, function)
        val providedByConstructor = getProvidedByConstructor(function)
        val resolvedDependency = dependencies.resolveDependency(function.returnType, function, providedByConstructor)
        injectionBuilder.buildModuleFunctionInjections(module, function, resolvedDependency)
        incrementalHelper?.recordFunctionDependency(function, resolvedDependency)
    }

    private fun getProvidedByConstructor(function: IrFunction): Set<IrType> {
        val inFunction = Annotations.getProvidedByConstructor(function)
        val inParentClasses = Utils.getParentClasses(function).flatMap { clazz ->
            providedByConstructorInClassCache.getOrPut(clazz) {
                Annotations.getProvidedByConstructor(clazz)
            }
        }
        val inFile = providedByConstructorInFileCache.getOrPut(function.file) {
            Annotations.getProvidedByConstructor(function.file)
        }
        return (inFile + inParentClasses + inFunction).toSet()
    }
}