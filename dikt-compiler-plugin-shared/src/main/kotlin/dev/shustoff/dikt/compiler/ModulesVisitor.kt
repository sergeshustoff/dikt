package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.*
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class ModulesVisitor(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = InjectionDependencyCollector(this)
    private val injectionBuilder = DependencyInjectionBuilder(pluginContext, errorCollector)

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (Annotations.isModule(declaration)) {
            if (!declaration.isFinalClass) {
                declaration.error("Module should be final")
            }
            val dependencies = dependencyCollector.collectDependencies(declaration)
            buildPropertyInjections(declaration, dependencies)
            RecursiveCallsDetector(errorCollector).checkForRecursiveCalls(declaration)
        }
        super.visitClass(declaration)
    }

    private fun buildPropertyInjections(
        declaration: IrClass,
        dependencies: ModuleDependencies
    ) {
        injectionBuilder.buildInjections(declaration, dependencies)
    }
}