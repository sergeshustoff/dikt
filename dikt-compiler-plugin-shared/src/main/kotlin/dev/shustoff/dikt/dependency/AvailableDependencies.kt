package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.VisibilityChecker
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor

data class AvailableDependencies(
    private val errorCollector: ErrorCollector,
    private val visibilityChecker: VisibilityChecker,
    private val dependencyMap: Map<DependencyId, List<Dependency>>,
) : ErrorCollector by errorCollector {

    fun resolveDependency(
        type: IrType,
        forFunction: IrFunction,
        providedByConstructor: Set<IrType>
    ): ResolvedDependency? {
        val isProvider = Annotations.isProvided(forFunction)
        return resolveDependencyInternal(
            DependencyId(type), Dependency.Function(forFunction, null), emptyList(),
            providedByConstructor = providedByConstructor + setOfNotNull(type.takeIf { !isProvider })
        )
    }

    private fun resolveDependencyInternal(
        id: DependencyId,
        forDependency: Dependency,
        usedTypes: List<IrType>,
        providedByConstructor: Set<IrType>
    ): ResolvedDependency? {
        val dependency = findDependency(id, forDependency, usedTypes, providedByConstructor)
            ?: return null
        val params = dependency.getRequiredParams()
        val extensionParam = dependency.getRequiredExtensionReceiver()
        val typeArgumentsMapping = buildTypeArgumentsMapping(id, dependency)
        val resolvedParams = getResolveParams(params, forDependency, usedTypes + id.type, typeArgumentsMapping, providedByConstructor)
        val resolvedExtensionParam = extensionParam?.let {
            getResolveParams(listOf(extensionParam), forDependency, usedTypes, typeArgumentsMapping, providedByConstructor)
        }
        val nestedChain = getResolveNestedChain(dependency, forDependency, usedTypes + id.type, typeArgumentsMapping, providedByConstructor)
        return resolvedParams
            ?.let {
                ResolvedDependency(dependency, nestedChain, it, resolvedExtensionParam?.firstOrNull())
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
        providedByConstructor: Set<IrType>,
    ): List<ResolvedDependency>? {
        return valueParameters
            .mapNotNull { param ->
                resolveDependencyInternal(
                    DependencyId(typeArgumentsMapping[param.type] ?: param.type),
                    forDependency,
                    usedTypes,
                    providedByConstructor = providedByConstructor,
                )
            }
            .takeIf { it.size == valueParameters.size } // errors reported in findDependency
    }

    private fun getResolveNestedChain(
        dependency: Dependency,
        forDependency: Dependency,
        usedTypes: List<IrType> = emptyList(),
        typeArgumentsMapping: Map<IrType?, IrType?>,
        providedByConstructor: Set<IrType>,
    ): ResolvedDependency? {
        return dependency.fromNestedModule?.let {
            resolveDependencyInternal(
                DependencyId(typeArgumentsMapping[it.id.type] ?: it.id.type),
                forDependency,
                usedTypes,
                providedByConstructor = providedByConstructor,
            )
        }
    }

    private fun findDependency(
        id: DependencyId,
        forDependency: Dependency,
        usedTypes: List<IrType> = emptyList(),
        providedByConstructor: Set<IrType> = emptySet()
    ): Dependency? {
        if (id.type in usedTypes) {
            forDependency.irElement.error(
                usedTypes.joinToString(prefix = "Recursive dependency: ",separator = " -> ") { it.asString() }
            )
            return null
        }

        return if (id.type in providedByConstructor) {
            getConstructorDependency(forDependency, id)
        } else {
            val dependencyOptions = dependencyMap[id].orEmpty() - forDependency
            // check local and nested in groups as well as parameterless and parameterized
            val result = getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule == null && it.getRequiredParams().isEmpty() && it.getRequiredExtensionReceiver() == null })
                ?: getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule == null && (it.getRequiredParams().isNotEmpty() || it.getRequiredExtensionReceiver() != null) })
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
        val clazz = id.type.getClass()
        val constructors = clazz?.primaryConstructor?.let { listOf(it) }
            ?: clazz?.constructors?.filter { visibilityChecker.isVisible(it) }.orEmpty().toList()

        if (constructors.isEmpty() || (constructors.size == 1 && !visibilityChecker.isVisible(constructors.first()))) {
            forDependency.irElement.error(
                "No visible constructor found for ${id.asErrorString()}",
            )
            return null
        } else if (constructors.size > 1) {
            forDependency.irElement.error(
                "Multiple visible constructors found for ${id.asErrorString()}",
            )
            return null
        }
        return Dependency.Constructor(constructors.first())
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