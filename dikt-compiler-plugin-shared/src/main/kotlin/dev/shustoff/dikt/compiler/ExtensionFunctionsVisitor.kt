package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.*
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class ExtensionFunctionsVisitor(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?,
    singletonGenerator: ModuleSingletonGenerator
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this, singletonGenerator)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    override fun visitElement(element: IrElement) {
        // modules are handled separately
        if (element !is IrClass || !Annotations.isModule(element)) {
            element.acceptChildren(this, null)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        if (Annotations.isProvidedByDi(declaration)) {
            if (Annotations.isSingleton(declaration)) {
                declaration.error("Singleton not supported in extension functions")
            } else {
                val dependencies = dependencyCollector.collectDependencies(
                    visibilityChecker = VisibilityChecker(declaration),
                    params = declaration.valueParameters + listOfNotNull(declaration.extensionReceiverParameter),
                )
                val dependency: ResolvedDependency? = dependencies.resolveDependency(declaration.returnType, declaration)
                injectionBuilder.buildExtensionInjection(declaration, dependency)
                incrementalHelper?.recordExtensionDependency(declaration, dependencies, dependency)
            }
        }

        super.visitFunction(declaration)
    }
}