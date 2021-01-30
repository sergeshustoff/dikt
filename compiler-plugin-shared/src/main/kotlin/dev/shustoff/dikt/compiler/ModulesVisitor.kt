package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.*
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.backend.jvm.codegen.psiElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class ModulesVisitor(
    private val errorCollector: ErrorCollector,
    private val moduleDeclarations: ModuleDeclarations,
    pluginContext: IrPluginContext,
    private val moduleSingletones: MutableMap<IrType, MutableList<IrClass>>
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = InjectionDependencyCollector(moduleDeclarations, this)
    private val injectionBuilder = DependencyInjectionBuilder(pluginContext)
    private val singletonesInitializer = SingletonesInitializer(pluginContext)

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (moduleDeclarations.isModule(declaration)) {
            if (!declaration.isFinalClass) {
                declaration.psiElement.error("Module should be final")
            }
            val singletones = moduleSingletones[declaration.defaultType]
            val diFunctionSymbols = getDiFunctionSymbols(declaration)
            val singletonDiFunction = getSingletonFunction(declaration)
            if (diFunctionSymbols.isEmpty() || singletonDiFunction == null) {
                declaration.psiElement.error(
                    "Can't read module declaration, something went horribly wrong"
                )
            } else {
                singletonesInitializer.initSingletones(declaration, singletones, singletonDiFunction)
                val dependencies = dependencyCollector.collectDependencies(declaration)
                buildPropertyInjections(declaration, diFunctionSymbols, dependencies)
                checkForIncorrectDiCalls(declaration, diFunctionSymbols)
            }
            RecursiveCallsDetector(errorCollector).checkForRecursiveCalls(declaration)
        }
        super.visitClass(declaration)
    }

    private fun buildPropertyInjections(
        declaration: IrClass,
        diFunctionSymbols: Set<IrSimpleFunctionSymbol>,
        dependencies: ModuleDependencies
    ) {
        injectionBuilder.buildPropertyInjections(declaration, diFunctionSymbols, dependencies)
    }

    private fun getDiFunctionSymbols(declaration: IrClass) = declaration.functions
        .filter { moduleDeclarations.isDiFunction(it) }
        .map { it.symbol }
        .toSet()

    private fun getSingletonFunction(declaration: IrClass) = declaration.functions
        .filter { moduleDeclarations.isSingleton(it) }
        .firstOrNull()

    private fun checkForIncorrectDiCalls(
        declaration: IrClass,
        diFunctionSymbols: Set<IrSimpleFunctionSymbol>
    ) {
        declaration.acceptChildren(InvalidDiCallDetector(errorCollector, diFunctionSymbols), null)
    }
}