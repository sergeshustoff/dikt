package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.jvm.ir.eraseTypeParameters
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties

class DependencyCollector(
    private val errorCollector: ErrorCollector
) {
    fun collectDependencies(
        module: IrClass,
        visibilityChecker: VisibilityChecker,
        properties: Sequence<IrProperty> = emptySequence(),
        functions: Sequence<IrSimpleFunction> = emptySequence(),
        params: List<IrValueParameter> = emptyList()
    ): ModuleDependencies {
        val fullDependencyMap: MutableMap<DependencyId, MutableList<Dependency>> = mutableMapOf()
        val moduleTypes = Annotations.getUsedModules(module)
            .mapNotNull { it.classOrNull?.defaultType } // for generics
            .toSet()

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

        fullDependencyMap.values.flatten()
            .mapNotNull {
                getModuleClassDescriptor(it, moduleTypes)
                    ?.let { classDescriptor -> Module(it, classDescriptor) }
            }
            .forEach { module ->
                val dependencies = getModuleRawProperties(module, visibilityChecker) +
                        getModuleRawFunctions(module, visibilityChecker)

                val withoutDuplicates = dependencies
                    .filter { fullDependencyMap[it.id]?.any { it.isInNestedModulePath(module.path) } != true }
                    .toList()

                withoutDuplicates.forEach { dependency ->
                    fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
                }
            }

        return ModuleDependencies(
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
}