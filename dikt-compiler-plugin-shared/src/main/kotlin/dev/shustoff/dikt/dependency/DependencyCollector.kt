package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.Utils
import dev.shustoff.dikt.utils.VisibilityChecker
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import java.util.*

class DependencyCollector(
    private val errorCollector: ErrorCollector
) : ErrorCollector by errorCollector {

    fun collectDependencies(module: IrClass, function: IrFunction): AvailableDependencies {
        val isCached = Annotations.isCached(function)
        if (isCached && function.valueParameters.isNotEmpty()) {
            function.error("Cached @Create functions should not have parameters")
        }
        return collectDependencies(
            visibilityChecker = VisibilityChecker(module),
            properties = module.properties,
            functions = module.functions,
            params = function.valueParameters.takeUnless { isCached }.orEmpty(),
            moduleTypes = getAllUseModulesTypes(function),
        )
    }

    private fun collectDependencies(
        visibilityChecker: VisibilityChecker,
        properties: Sequence<IrProperty> = emptySequence(),
        functions: Sequence<IrSimpleFunction> = emptySequence(),
        params: List<IrValueParameter> = emptyList(),
        moduleTypes: Set<IrType> = emptySet()
    ): AvailableDependencies {
        val fullDependencyMap: MutableMap<DependencyId, MutableList<Dependency>> = mutableMapOf()
        properties
            .forEach { property ->
                val dependency = Dependency.Property(property, null)
                fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
            }

        functions.forEach { function ->
            createFunctionDependency(function)?.let { dependency ->
                fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
            }
        }
        params.forEach {
            val dependency = Dependency.Parameter(it)
            fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
        }

        val modules = LinkedList(fullDependencyMap.values.flatten()
            .mapNotNull {
                getModuleClassDescriptor(it, moduleTypes)
                    ?.let { classDescriptor -> Module(it, classDescriptor) }
            }
            .toList()
        )

        while (modules.isNotEmpty()) {
            val module = modules.pop()
            val dependencies = getModuleRawProperties(module, visibilityChecker) +
                    getModuleRawFunctions(module, visibilityChecker)

            val withoutDuplicates = dependencies
                .filter { fullDependencyMap[it.id]?.any { it.isInNestedModulePath(module.path) } != true }
                .toList()

            withoutDuplicates.forEach { dependency ->
                fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
            }

            modules.addAll(
                withoutDuplicates.mapNotNull {
                    getModuleClassDescriptor(it, moduleTypes)
                        ?.let { classDescriptor -> Module(it, classDescriptor) }
                }.toList()
            )
        }

        return AvailableDependencies(
            errorCollector,
            visibilityChecker,
            fullDependencyMap,
        )
    }

    private fun getModuleRawProperties(
        module: Module,
        visibilityChecker: VisibilityChecker
    ): Sequence<Dependency.Property> = module.clazz.properties
        .filter { visibilityChecker.isVisible(it) }
        .map {
            Dependency.Property(
                it,
                module.path,
                returnType = module.typeMap[it.getter!!.returnType] ?: it.getter!!.returnType
            )
        }

    private fun getModuleRawFunctions(
        module: Module,
        visibilityChecker: VisibilityChecker
    ) = module.clazz.functions
        .filter { visibilityChecker.isVisible(it) }
        .mapNotNull { createFunctionDependency(it, module) }

    private fun createFunctionDependency(it: IrSimpleFunction, module: Module? = null): Dependency.Function? {
        if (!isDependencyFunction(it)) return null
        return Dependency.Function(it, module?.path, returnType = module?.typeMap?.get(it.returnType) ?: it.returnType)
    }

    private fun isDependencyFunction(it: IrSimpleFunction) =
        !it.isFakeOverride && !it.isOperator && !it.isSuspend && !it.isInfix && !it.returnType.isUnit() && !it.returnType.isNothing()

    private fun getModuleClassDescriptor(dependency: Dependency, moduleTypes: Set<IrType>) =
        dependency.id.type.takeIf { it.classOrNull?.defaultType in moduleTypes }?.getClass()

    private data class Module(
        val path: Dependency,
        val clazz: IrClass,
    ) {
        val typeMap: Map<IrType?, IrType?> by lazy {
            val typeArguments = (path.id.type as? IrSimpleType)?.arguments?.map { it as? IrType }
            val dependencyTypeArguments = (clazz.defaultType as? IrSimpleType)?.arguments?.map { it as? IrType }
            dependencyTypeArguments?.zip(typeArguments.orEmpty())?.toMap().orEmpty()
        }
    }

    companion object {
        private fun getAllUseModulesTypes(function: IrFunction): Set<IrType> {
            val classes =  Utils.getParentClasses(function)
            val fromClasses = classes.flatMap { Annotations.getUsedModules(it) }
            val fromFile = Annotations.getUsedModules(function.file)
            val fromFunction = Annotations.getUsedModules(function)
            return (fromFile + fromClasses + fromFunction)
                .mapNotNull { it.classOrNull?.defaultType } // for generics
                .toSet()
        }
    }
}