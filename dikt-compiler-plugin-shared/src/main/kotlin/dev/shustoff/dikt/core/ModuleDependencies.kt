package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.primaryConstructor

class ModuleDependencies(
    errorCollector: ErrorCollector,
    private val visibilityChecker: VisibilityChecker,
    private val dependencyMap: Map<DependencyId, List<Dependency>>,
) : ErrorCollector by errorCollector {

    fun resolveDependency(
        type: IrType,
        forFunction: IrFunction
    ): ResolvedDependency? {
        val isProvider = Annotations.isProviderForExternalDependency(forFunction)
        val isCached = Annotations.isCached(forFunction)
        if (isCached && forFunction.valueParameters.isNotEmpty()) {
            forFunction.error("Cached @Create functions should not have parameters")
        }
        val params = forFunction.valueParameters.takeUnless { isCached }
            ?.associate { Dependency.Parameter(it).let { it.id to it } }
            .orEmpty()
        return resolveDependencyInternal(DependencyId(type), Dependency.Function(forFunction, null), emptyList(), params,
            useConstructor = !isProvider)
    }

    private fun resolveDependencyInternal(
        id: DependencyId,
        forDependency: Dependency,
        usedTypes: List<IrType>,
        providedParams: Map<DependencyId, Dependency>,
        useConstructor: Boolean = false
    ): ResolvedDependency? {
        val dependency = providedParams[id]?.takeUnless { useConstructor }
            ?: findDependency(id, forDependency, usedTypes, useConstructor)
            ?: return null
        val params = dependency.getRequiredParams()
        val typeArgumentsMapping = buildTypeArgumentsMapping(id, dependency)
        val resolvedParams = getResolveParams(params, forDependency, usedTypes + id.type, typeArgumentsMapping, providedParams)
        val nestedChain = getResolveNestedChain(dependency, forDependency, usedTypes + id.type, typeArgumentsMapping, providedParams)
        return resolvedParams
            ?.let {
                ResolvedDependency(dependency, nestedChain, it)
            }
    }

    private fun buildTypeArgumentsMapping(
        id: DependencyId,
        dependency: Dependency
    ): Map<IrType?, IrType?> {
        val typeArguments = (id.type as? IrSimpleType)?.arguments?.map { it as? IrType }
        val dependencyTypeArguments = (dependency.id.type as? IrSimpleType)?.arguments?.map { it as? IrType }
        return dependencyTypeArguments.orEmpty().zip(typeArguments.orEmpty()).toMap()
    }

    private fun getResolveParams(
        valueParameters: List<IrValueParameter>,
        forDependency: Dependency,
        usedTypes: List<IrType> = emptyList(),
        typeArgumentsMapping: Map<IrType?, IrType?>,
        providedParams: Map<DependencyId, Dependency>,
    ): List<ResolvedDependency>? {
        return valueParameters
            .mapNotNull { param ->
                resolveDependencyInternal(
                    DependencyId(typeArgumentsMapping[param.type] ?: param.type),
                    forDependency,
                    usedTypes,
                    providedParams,
                )
            }
            .takeIf { it.size == valueParameters.size } // errors reported in findDependency
    }

    private fun getResolveNestedChain(
        dependency: Dependency,
        forDependency: Dependency,
        usedTypes: List<IrType> = emptyList(),
        typeArgumentsMapping: Map<IrType?, IrType?>,
        providedParams: Map<DependencyId, Dependency>,
    ): ResolvedDependency? {
        return dependency.fromNestedModule?.let {
            resolveDependencyInternal(
                DependencyId(typeArgumentsMapping[it.id.type] ?: it.id.type),
                forDependency,
                usedTypes,
                providedParams,
            )
        }
    }

    private fun findDependency(
        id: DependencyId,
        forDependency: Dependency,
        usedTypes: List<IrType> = emptyList(),
        useConstructor: Boolean = false
    ): Dependency? {
        if (id.type in usedTypes) {
            forDependency.irElement.error(
                usedTypes.joinToString(prefix = "Recursive dependency: ",separator = " -> ") { it.asString() }
            )
            return null
        }

        return if (useConstructor) {
            getConstructorDependency(forDependency, id)
        } else {
            val dependencyOptions = dependencyMap[id].orEmpty() - forDependency
            // check local and nested in groups as well as parameterless and parameterized
            val result = getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule == null && it.getRequiredParams().isEmpty() })
                ?: getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule == null && it.getRequiredParams().isNotEmpty() })
                ?: getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule != null })

            if (result == null) {
                forDependency.irElement.error(
                    "Can't resolve dependency ${id.asErrorString()}",
                )
            }

            result
        }
    }

    private fun getConstructorDependency(forDependency: Dependency, id: DependencyId): Dependency? {
        val constructor = id.type.getClass()?.primaryConstructor

        if (constructor == null || !visibilityChecker.isVisible(constructor)) {
            forDependency.irElement.error(
                "No visible constructor found for ${id.asErrorString()}",
            )
            return null
        }
        return Dependency.Constructor(constructor)
    }

    private fun getDependencyFromGroup(forDependency: Dependency, id: DependencyId, options: List<Dependency>): Dependency? {
        if (options.isNotEmpty()) {
            if (options.size > 1) {
                forDependency.irElement.error(
                    options.mapNotNull { it.nameWithNestedChain() }.joinToString(prefix = "Multiple dependencies provided with type ${id.asErrorString()}: ")
                )
            }
            return options.first()
        }
        return null
    }
}