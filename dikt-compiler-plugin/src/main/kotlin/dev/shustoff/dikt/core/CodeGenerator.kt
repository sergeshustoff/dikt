package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.AvailableDependencies
import dev.shustoff.dikt.dependency.DependencyCollector
import dev.shustoff.dikt.dependency.ProvidedDependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.Utils
import dev.shustoff.dikt.utils.VisibilityChecker
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getSourceLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.doNothing
import java.util.*
import kotlin.collections.ArrayDeque

class CodeGenerator(
    private val errorCollector: ErrorCollector,
    pluginContext: IrPluginContext,
    private val incrementalHelper: IncrementalCompilationHelper?,
) : IrElementTransformer<CodeGenerator.Data>, ErrorCollector by errorCollector {

    private val dependencyCollector = DependencyCollector(this)
    private val injectionBuilder = InjectionBuilder(pluginContext, errorCollector)

    private val providedByConstructorInClassCache = mutableMapOf<IrClass, List<IrType>>()
    private val providedByConstructorInFileCache = mutableMapOf<IrFile, List<IrType>>()
    private val dependencyByModuleCache = mutableMapOf<IrClass, AvailableDependencies>()

    override fun visitFile(declaration: IrFile, data: Data): IrFile {
        return super.visitFile(declaration, data.copy(file = declaration))
    }

    override fun visitClass(declaration: IrClass, data: Data): IrStatement {
        val module = ModuleData(declaration)
        return super.visitClass(declaration, data.copy(module = module)).also {
            checkForRecursiveMembersCalls(module)
        }
    }

    private fun checkForRecursiveMembersCalls(module: ModuleData) {
        val usedModuleDeclarationsByFunction = buildInModuleDependencyMap(module)

        findRecursiveCalls(usedModuleDeclarationsByFunction, module.irClass)
    }

    private fun buildInModuleDependencyMap(module: ModuleData) =
        module.dependencies.mapValues { (_, resolved) ->
            val queue = ArrayDeque(resolved)
            val usedModuleDeclarations = mutableSetOf<IrDeclarationWithName>()
            while (queue.isNotEmpty()) {
                when (val item = queue.removeFirst()) {
                    is ResolvedDependency.Constructor -> queue.addAll(item.params)
                    is ResolvedDependency.ParameterDefaultValue -> doNothing()
                    is ResolvedDependency.Provided -> {
                        queue.addAll(item.params)
                        if (item.nestedModulesChain != null) queue.add(item.nestedModulesChain)
                        if (item.extensionParam != null) queue.add(item.extensionParam)

                        if (item.provided.fromNestedModule == null) {
                            usedModuleDeclarations.add(item.provided.irElement)
                        }
                    }
                }
            }
            usedModuleDeclarations
        }

    private fun findRecursiveCalls(
        dependencyMap: Map<IrDeclarationWithName, Set<IrDeclarationWithName>>,
        module: IrClass
    ) {
        val nodes = dependencyMap
            .filterValues { it.isNotEmpty() }

        val inDegreeCount = nodes.mapValues { 0 }.toMutableMap()
        for (node in nodes.values) {
            for (param in node) {
                if (inDegreeCount.containsKey(param)) {
                    inDegreeCount[param] = inDegreeCount[param]!! + 1
                }
            }
        }
        val queue = LinkedList(inDegreeCount.filterValues { it == 0 }.keys)
        while (queue.isNotEmpty()) {
            val from = queue.pop()
            nodes[from]?.forEach { to ->
                if (inDegreeCount.containsKey(to)) {
                    inDegreeCount[to] = inDegreeCount[to]!! - 1
                    if (inDegreeCount[to] == 0) {
                        queue.add(to)
                    }
                }
            }
        }
        val declarationsWithCycles = inDegreeCount.filterValues { it > 0 }.keys
        if (declarationsWithCycles.isNotEmpty()) {
            module.error(declarationsWithCycles.joinToString(prefix = "Recursive dependency detected: ") { it.name.asString() })
        }
        for (declaration in declarationsWithCycles) {
            declaration.error("Recursive dependency detected")
        }
    }

    override fun visitFunction(declaration: IrFunction, data: Data): IrStatement {
        return super.visitFunction(declaration, data.copy(function = declaration))
    }

    override fun visitProperty(declaration: IrProperty, data: Data): IrStatement {
        return super.visitProperty(declaration, data.copy(property = declaration))
    }

    override fun visitExpression(expression: IrExpression, data: Data): IrExpression {
        if (expression is IrCall && isDiFunction(expression)) {
            if (data.function == null || data.property != null) {
                error("Dependency can only be resolved inside functions and getters", expression.getSourceLocation(data.file))
                return super.visitExpression(expression, data)
            } else {
                val resolvedDependency = resolveDependency(data.module?.irClass, data.function, expression)
                incrementalHelper?.recordFunctionDependency(data.function, resolvedDependency)
                val resolvedExpression = injectionBuilder.buildResolvedDependencyCall(data.module?.irClass, data.function, resolvedDependency, expression)

                if (resolvedDependency != null) {
                    data.module?.registerDependency(data.function, resolvedDependency)
                }

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
    ): ResolvedDependency? {
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
        return dependencies.resolveDependency(original.type, function, providedByConstructor, singletons, moduleScopes)
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
        expression.symbol.owner.getPackageFragment().packageFqName == diktPackage &&
                expression.symbol.owner.name.identifier == diFunctionName

    data class Data(
        val file: IrFile? = null,
        val module: ModuleData? = null,
        val property: IrProperty? = null,
        val function: IrFunction? = null,
    )

    data class ModuleData(
        val irClass: IrClass,
    ) {
        private val _dependencies = mutableMapOf<IrDeclarationWithName, MutableList<ResolvedDependency>>()

        val dependencies: Map<IrDeclarationWithName, List<ResolvedDependency>> = _dependencies

        fun registerDependency(source: IrFunction, dependency: ResolvedDependency) {
            _dependencies?.getOrPut(source) { mutableListOf() }?.add(dependency)
        }
    }

    companion object {
        val diFunctionName = "resolve"
        val diktPackage = FqName("dev.shustoff.dikt")
    }
}