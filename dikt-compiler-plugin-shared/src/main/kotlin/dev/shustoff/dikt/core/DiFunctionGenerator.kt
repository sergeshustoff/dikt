package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.AvailableDependencies
import dev.shustoff.dikt.dependency.DependencyCollector
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.Utils
import dev.shustoff.dikt.utils.VisibilityChecker
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class DiFunctionGenerator(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?,
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    private val providedByConstructorInClassCache = mutableMapOf<IrClass, List<IrType>>()
    private val providedByConstructorInFileCache = mutableMapOf<IrFile, List<IrType>>()
    private val dependencyByModuleCache = mutableMapOf<IrClass, AvailableDependencies>()

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        val diFunctions = declaration.functions.filter { Annotations.isCached(it) }.toList()
        diFunctions.forEach { function ->
            buildFunctionBody(declaration, function)
        }
        super.visitClass(declaration)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (Annotations.isByDi(declaration)) {
            if (Annotations.isCached(declaration)) {
                // cached functions handled in visitClass
                if (declaration.parentClassOrNull == null) {
                    declaration.error("Cached @Create functions without parent class not supported")
                }
            } else {
                buildFunctionBody(declaration.parentClassOrNull, declaration)
            }
        }

        super.visitFunction(declaration)
    }

    private fun buildFunctionBody(
        module: IrClass?,
        function: IrFunction
    ) {
        if ((function as? IrSimpleFunction)?.modality != Modality.FINAL) {
            function.error("Only final functions can have generated body")
            return
        }
        val dependencies = if (module != null && function.valueParameters.isEmpty() && Annotations.getUsedModules(function).isEmpty()) {
            // in most cases we don't need to resolve dependencies again
            dependencyByModuleCache.getOrPut(module) {
                dependencyCollector.collectDependencies(module, function)
            }
        } else {
            dependencyCollector.collectDependencies(module, function)
        }.copy(visibilityChecker = VisibilityChecker(function))

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