package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.VisibilityChecker
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor

data class AvailableDependencies(
    private val errorCollector: ErrorCollector,
    private val visibilityChecker: VisibilityChecker,
    private val dependencyMap: Map<DependencyId, List<ProvidedDependency>>,
) : ErrorCollector by errorCollector {
//TODO: clean this up, it's unreadable
    fun resolveDependency(
        type: IrType,
        forFunction: IrFunction,
        providedByConstructor: Set<IrType>,
        singletons: Set<IrType>
    ): ResolvedDependency? {
        return resolveDependencyInternal(
            DependencyId(type), forFunction, emptyList(),
            providedByConstructor = providedByConstructor,
            singletons = singletons,
        )
    }

    private fun resolveDependencyInternal(
        id: DependencyId,
        forFunction: IrFunction,
        usedTypes: List<IrType>,
        providedByConstructor: Set<IrType>,
        defaultValue: IrExpressionBody? = null,
        singletons: Set<IrType>,
        forbidFunctionParams: Boolean = false
    ): ResolvedDependency? {
        if (id.type in usedTypes) {
            forFunction.error(
                usedTypes.joinToString(prefix = "Recursive dependency: ",separator = " -> ") { it.asString() }
            )
            return null
        }

        //TODO: need check type without generic params somehow here
        val isSingleton = id.type in singletons
        if (isSingleton || id.type in providedByConstructor) {
            return buildResolvedConstructor(forFunction, id, usedTypes, providedByConstructor, singletons, forbidFunctionParams = forbidFunctionParams || isSingleton)
        } else {
            val dependency = findProvidedDependency(id, forFunction)
                ?.takeIf { !forbidFunctionParams || it !is ProvidedDependency.Parameter }
            if (dependency == null) {
                if (defaultValue == null) {
                    forFunction.error(
                        "Can't resolve dependency ${id.asErrorString()}",
                    )
                    return null
                } else {
                    return ResolvedDependency.ParameterDefaultValue(id.type, defaultValue)
                }
            } else {
                return resolveProvidedDependency(id, forFunction, dependency, usedTypes, providedByConstructor, singletons)
            }
        }
    }

    private fun resolveProvidedDependency(
        id: DependencyId,
        forFunction: IrFunction,
        dependency: ProvidedDependency,
        usedTypes: List<IrType>,
        providedByConstructor: Set<IrType>,
        singletons: Set<IrType>
    ): ResolvedDependency.Provided? {
        val params = dependency.getRequiredParams()
        val extensionParam = dependency.getRequiredExtensionReceiver()
        val typeArgumentsMapping = buildTypeArgumentsMapping(id, dependency.id.type)
        val resolvedParams = getResolveParams(
            params,
            forFunction,
            usedTypes + id.type,
            typeArgumentsMapping,
            providedByConstructor,
            singletons,
        )
        val resolvedExtensionParam = extensionParam?.let {
            getResolveParams(
                listOf(extensionParam),
                forFunction,
                usedTypes,
                typeArgumentsMapping,
                providedByConstructor,
                singletons,
            )
        }
        val nestedChain = getResolveNestedChain(
            dependency,
            forFunction,
            usedTypes + id.type,
            typeArgumentsMapping,
            providedByConstructor,
            singletons,
        )
        return resolvedParams
            ?.let {
                ResolvedDependency.Provided(dependency, nestedChain, it, resolvedExtensionParam?.firstOrNull())
            }
    }

    private fun findProvidedDependency(
        id: DependencyId,
        forFunction: IrFunction
    ): ProvidedDependency? {
        val dependencyOptions = dependencyMap[id].orEmpty().filter { it.irElement !== forFunction }
        // check local and nested in groups as well as parameterless and parameterized
        val dependency = getDependencyFromGroup(
            forFunction,
            id,
            dependencyOptions.filter {
                it.fromNestedModule == null && it.getRequiredParams()
                    .isEmpty() && it.getRequiredExtensionReceiver() == null
            })
            ?: getDependencyFromGroup(
                forFunction,
                id,
                dependencyOptions.filter {
                    it.fromNestedModule == null && (it.getRequiredParams()
                        .isNotEmpty() || it.getRequiredExtensionReceiver() != null)
                })
            ?: getDependencyFromGroup(forFunction, id, dependencyOptions.filter { it.fromNestedModule != null })
        return dependency
    }

    private fun buildTypeArgumentsMapping(
        id: DependencyId,
        providedType: IrType,
    ): Map<IrType?, IrType?> {
        val typeArguments = (id.type as? IrSimpleType)?.arguments?.map { it as? IrType }
        val dependencyTypeArguments = (providedType as? IrSimpleType)?.arguments?.map { it as? IrType }
        return dependencyTypeArguments.orEmpty().zip(typeArguments.orEmpty()).toMap()
    }

    private fun getResolveParams(
        valueParameters: List<IrValueParameter>,
        forFunction: IrFunction,
        usedTypes: List<IrType> = emptyList(),
        typeArgumentsMapping: Map<IrType?, IrType?>,
        providedByConstructor: Set<IrType>,
        singletons: Set<IrType>,
        forbidFunctionParams: Boolean = false,
    ): List<ResolvedDependency>? {
        return valueParameters
            .mapNotNull { param ->
                resolveDependencyInternal(
                    DependencyId(typeArgumentsMapping[param.type] ?: param.type),
                    forFunction,
                    usedTypes,
                    providedByConstructor = providedByConstructor,
                    defaultValue = param.defaultValue,
                    singletons = singletons,
                    forbidFunctionParams = forbidFunctionParams
                )
            }
            .takeIf { it.size == valueParameters.size } // errors reported in resolveDependencyInternal
    }

    private fun getResolveNestedChain(
        dependency: ProvidedDependency,
        forFunction: IrFunction,
        usedTypes: List<IrType> = emptyList(),
        typeArgumentsMapping: Map<IrType?, IrType?>,
        providedByConstructor: Set<IrType>,
        singletons: Set<IrType>,
    ): ResolvedDependency? {
        return dependency.fromNestedModule?.let {
            resolveDependencyInternal(
                DependencyId(typeArgumentsMapping[it.id.type] ?: it.id.type),
                forFunction,
                usedTypes,
                providedByConstructor = providedByConstructor,
                singletons = singletons,
            )
        }
    }

    private fun buildResolvedConstructor(
        forFunction: IrFunction,
        id: DependencyId,
        usedTypes: List<IrType>,
        providedByConstructor: Set<IrType>,
        singletons: Set<IrType>,
        forbidFunctionParams: Boolean = false
    ): ResolvedDependency? {
        val clazz = id.type.getClass()
        val constructors = clazz?.primaryConstructor?.let { listOf(it) }
            ?: clazz?.constructors?.filter { visibilityChecker.isVisible(it) }.orEmpty().toList()

        if (constructors.isEmpty() || (constructors.size == 1 && !visibilityChecker.isVisible(constructors.first()))) {
            forFunction.error(
                "No visible constructor found for ${id.asErrorString()}",
            )
            return null
        } else if (constructors.size > 1) {
            forFunction.error(
                "Multiple visible constructors found for ${id.asErrorString()}",
            )
            return null
        }
        val constructor = constructors.first()
        val typeArgumentsMapping = buildTypeArgumentsMapping(id, constructor.returnType)
        val resolvedParams = getResolveParams(
            constructor.valueParameters,
            forFunction,
            usedTypes + id.type,
            typeArgumentsMapping,
            providedByConstructor,
            singletons,
            forbidFunctionParams = forbidFunctionParams
        )
            ?: return null

        return ResolvedDependency.Constructor(id.type, constructor, resolvedParams, isSingleton = id.type in singletons)
    }

    private fun getDependencyFromGroup(forFunction: IrFunction, id: DependencyId, options: List<ProvidedDependency>): ProvidedDependency? {
        if (options.isNotEmpty()) {
            if (options.size > 1) {
                forFunction.error(
                    options.mapNotNull { it.nameWithNestedChain() }.joinToString(prefix = "Multiple dependencies provided with type ${id.asErrorString()}: ")
                )
            }
            return options.first()
        }
        return null
    }
}