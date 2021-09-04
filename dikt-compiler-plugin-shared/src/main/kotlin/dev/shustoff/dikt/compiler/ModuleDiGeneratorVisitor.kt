package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.*
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class ModuleDiGeneratorVisitor(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (Annotations.isModule(declaration)) {
            if (declaration.isInterface) {
                declaration.error("Interface modules not supported")
            } else if (!declaration.isFinalClass) {
                declaration.error("Module should be final")
            } else {
                val providedByConstructorInModule = Annotations.getProvidedByConstructor(declaration)
                val dependencies = dependencyCollector.collectDependencies(
                    visibilityChecker = VisibilityChecker(declaration),
                    properties = declaration.properties,
                    functions = declaration.functions
                )
                val diFunctions = declaration.functions
                    .filter { function -> Annotations.isProvidedByDi(function) }
                    .map { function ->
                        val providedByConstructor = providedByConstructorInModule + Annotations.getProvidedByConstructor(function)
                        function to dependencies.resolveDependency(function.returnType, function, providedByConstructor) }
                    .toList()

                diFunctions.forEach { (function, dependency) ->
                    injectionBuilder.buildModuleFunctionInjections(declaration, function, dependency)
                }

                incrementalHelper?.recordModuleDependency(declaration, diFunctions.mapNotNull { it.second })
                RecursiveCallsDetector(errorCollector).checkForRecursiveCalls(declaration)
            }
        }
        super.visitClass(declaration)
    }
}