package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.AvailableDependencies
import dev.shustoff.dikt.dependency.DependencyCollector
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.Utils
import dev.shustoff.dikt.utils.VisibilityChecker
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName

class DiNewApiCodeGenerator(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?,
) : IrElementTransformer<DiNewApiCodeGenerator.Data>, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    private val providedByConstructorInClassCache = mutableMapOf<IrClass, List<IrType>>()
    private val providedByConstructorInFileCache = mutableMapOf<IrFile, List<IrType>>()
    private val dependencyByModuleCache = mutableMapOf<IrClass, AvailableDependencies>()

    override fun visitClass(declaration: IrClass, data: Data): IrStatement {
        return super.visitClass(declaration, data.copy(module = declaration))
    }

    override fun visitFunction(declaration: IrFunction, data: Data): IrStatement {
        return super.visitFunction(declaration, data.copy(function = declaration))
    }

    override fun visitExpression(expression: IrExpression, data: Data): IrExpression {
        if (expression is IrCall && isDiFunction(expression)) {
            if (data.function == null) {
                expression.symbol.owner.error("Dependency can only be resolved inside functions for now")
                return super.visitExpression(expression, data)
            } else {
                val resolvedExpression = resolveDependency(data.module, data.function, expression)
                return super.visitExpression(resolvedExpression, data)
            }
        } else {
            return super.visitExpression(expression, data)
        }
    }

    private fun resolveDependency(
        module: IrClass?,
        function: IrFunction,
        original: IrCall
    ): IrExpression {
        val dependencies = if (module != null &&
            function.valueParameters.isEmpty() &&
            function.extensionReceiverParameter == null &&
            Annotations.getUsedModules(function).isEmpty()
        ) {
            // in most cases we don't need to resolve dependencies again
            dependencyByModuleCache.getOrPut(module) {
                dependencyCollector.collectDependencies(module, function)
            }
        } else {
            dependencyCollector.collectDependencies(module, function)
        }.copy(visibilityChecker = VisibilityChecker(function))

        val singletons = module?.let { Annotations.singletonsByConstructor(module) }.orEmpty()
        val providedByConstructor = getProvidedByConstructor(function)
        val moduleScopes = module?.let { Annotations.getModuleScopes(module) }.orEmpty()
        val resolvedDependency = dependencies.resolveDependency(original.type, function, providedByConstructor, singletons, moduleScopes)
        incrementalHelper?.recordFunctionDependency(function, resolvedDependency)
        return injectionBuilder.buildResolvedDependencyCall(module, function, resolvedDependency, original)
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

    private fun isDiFunction(expression: IrCall) =
        expression.symbol.owner.getPackageFragment().fqName == diktPackage &&
                expression.symbol.owner.name.identifier == diFunctionName

    data class Data(
        val module: IrClass? = null,
        val function: IrFunction? = null,
    )

    companion object {
        val diFunctionName = "resolve"
        val diktPackage = FqName("dev.shustoff.dikt")
    }
}