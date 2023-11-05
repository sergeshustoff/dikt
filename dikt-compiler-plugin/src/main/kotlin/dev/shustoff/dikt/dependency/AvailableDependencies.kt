package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import dev.shustoff.dikt.utils.VisibilityChecker
import org.jetbrains.kotlin.backend.jvm.codegen.anyTypeArgument
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor

data class AvailableDependencies(
    private val errorCollector: ErrorCollector,
    private val visibilityChecker: VisibilityChecker,
    private val dependencyMap: Map<DependencyId, List<ProvidedDependency>>,
) : ErrorCollector by errorCollector {
    fun resolveDependency(
        type: IrType,
        forFunction: IrFunction,
        providedByConstructor: Set<IrType>,
        singletons: Set<IrType>,
        moduleScopes: Set<IrType>
    ): ResolvedDependency? {
        return resolveDependencyInternal(
            DependencyId(type),
            context = Context(
                forFunction = forFunction,
                usedTypes = emptyList(),
                providedByConstructor = providedByConstructor,
                singletons = singletons,
                forbidFunctionParams = false,
                moduleScopes = moduleScopes,
            )
        )
    }

    private fun resolveDependencyInternal(
        id: DependencyId,
        defaultValue: IrExpressionBody? = null,
        context: Context
    ): ResolvedDependency? {
        if (id.type in context.usedTypes) {
            context.forFunction.error(
                context.usedTypes.joinToString(prefix = "Recursive dependency: ",separator = " -> ") { it.classFqName!!.asString() }
            )
            return null
        }

        val isClassInSingletonList = id.type.classOrNull?.defaultType in context.singletons ||
                Annotations.isInjectableSingletonInScopes(id.type, context.moduleScopes)
        if (isClassInSingletonList && id.type.anyTypeArgument { true }) {
            context.forFunction.error("Generic types can't be singletons")
        }
        val isSingleton = isClassInSingletonList && !id.type.anyTypeArgument { true }

        val providedDependency = findProvidedDependency(id, context.forFunction)
            ?.takeIf { !context.forbidFunctionParams || it !is ProvidedDependency.Parameter }

        val canInjectByConstructor = isClassInSingletonList ||
                id.type.classOrNull?.defaultType in context.providedByConstructor ||
                Annotations.isInjectable(id.type)

        // only inject by constructor if not already provided from nested module
        if (canInjectByConstructor && providedDependency?.fromNestedModule == null) {
            return buildResolvedConstructor(id, context, isSingleton)
        } else if (providedDependency != null) {
            // TODO: maybe return something even in case of error to postpone throwing it? In this case we can try different type of injection
            return resolveProvidedDependency(id, providedDependency, context)
        } else if (defaultValue != null) {
            return ResolvedDependency.ParameterDefaultValue(id.type, defaultValue)
        } else {
            context.forFunction.error("Can't resolve dependency ${id.asErrorString()}")
            return null
        }
    }

    private fun resolveProvidedDependency(
        id: DependencyId,
        dependency: ProvidedDependency,
        context: Context
    ): ResolvedDependency.Provided? {
        val params = dependency.getRequiredParams()
        val extensionParam = dependency.getRequiredExtensionReceiver()
        val typeArgumentsMapping = buildTypeArgumentsMapping(id, dependency.id.type)
        val newContext = context.copy(usedTypes = context.usedTypes + id.type)
        val resolvedParams = getResolveParams(
            context = newContext,
            params,
            typeArgumentsMapping,
        )
        val resolvedExtensionParam = extensionParam?.let {
            getResolveParams(
                context = newContext,
                listOf(extensionParam),
                typeArgumentsMapping,
            )
        }
        val nestedChain = getResolveNestedChain(
            context = newContext,
            dependency,
            typeArgumentsMapping,
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
        val rawDependencyOptions = dependencyMap[id].orEmpty().filter { it.irElement !== forFunction }

        val dependencyOptions = if (id.type.isNullable() && rawDependencyOptions.isEmpty()) {
            val nonNullableId = DependencyId(id.type.makeNotNull())
            dependencyMap[nonNullableId].orEmpty().filter { it.irElement !== forFunction }
        } else {
            rawDependencyOptions
        }

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
        context: Context,
        valueParameters: List<IrValueParameter>,
        typeArgumentsMapping: Map<IrType?, IrType?>,
    ): List<ResolvedDependency>? {
        return valueParameters
            .mapNotNull { param ->
                resolveDependencyInternal(
                    DependencyId(typeArgumentsMapping[param.type] ?: param.type),
                    defaultValue = param.defaultValue,
                    context = context
                )
            }
            .takeIf { it.size == valueParameters.size } // errors reported in resolveDependencyInternal
    }

    private fun getResolveNestedChain(
        context: Context,
        dependency: ProvidedDependency,
        typeArgumentsMapping: Map<IrType?, IrType?>,
    ): ResolvedDependency? {
        return dependency.fromNestedModule?.let {
            resolveDependencyInternal(
                DependencyId(typeArgumentsMapping[it.id.type] ?: it.id.type),
                context = context
            )
        }
    }

    private fun buildResolvedConstructor(
        id: DependencyId,
        context: Context,
        isSingleton: Boolean = false,
    ): ResolvedDependency? {
        val clazz = id.type.getClass()
        val constructors = clazz?.primaryConstructor?.let { listOf(it) }
            ?: clazz?.constructors?.filter { visibilityChecker.isVisible(it) }.orEmpty().toList()

        if (constructors.isEmpty() || (constructors.size == 1 && !visibilityChecker.isVisible(constructors.first()))) {
            context.forFunction.error(
                "No visible constructor found for ${id.asErrorString()}",
            )
            return null
        } else if (constructors.size > 1) {
            context.forFunction.error(
                "Multiple visible constructors found for ${id.asErrorString()}",
            )
            return null
        }
        val constructor = constructors.first()
        val typeArgumentsMapping = buildTypeArgumentsMapping(id, constructor.returnType)
        val resolvedParams = getResolveParams(
            context.copy(
                usedTypes = context.usedTypes + id.type,
                forbidFunctionParams = context.forbidFunctionParams || isSingleton
            ),
            constructor.valueParameters,
            typeArgumentsMapping
        )
            ?: return null

        return ResolvedDependency.Constructor(id.type, constructor, resolvedParams, isSingleton = isSingleton)
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

    private data class Context(
        val forFunction: IrFunction,
        val usedTypes: List<IrType>,
        val providedByConstructor: Set<IrType>,
        val singletons: Set<IrType>,
        val forbidFunctionParams: Boolean,
        val moduleScopes: Set<IrType>,
    )
}