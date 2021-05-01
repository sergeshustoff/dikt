package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.primaryConstructor

class ModuleDependencies(
    errorCollector: ErrorCollector,
    private val module: IrClass,
    private val dependencyMap: Map<DependencyId, List<Dependency>>
) : ErrorCollector by errorCollector {

    fun resolveDependency(
        type: IrType,
        forFunction: IrSimpleFunction
    ): ResolvedDependency? {
        val params = forFunction.valueParameters.associate { Dependency.Parameter(it).let { it.id to it } }
        return resolveDependencyInternal(DependencyId(type), Dependency.Function(forFunction, null), emptySet(), params,
            allowConstructorWithoutAnnotation = true)
    }

    private fun resolveDependencyInternal(
        id: DependencyId,
        forDependency: Dependency,
        usedTypes: Set<IrType>,
        providedParams: Map<DependencyId, Dependency>,
        allowConstructorWithoutAnnotation: Boolean = false
    ): ResolvedDependency? {
        val dependency = providedParams[id]
            ?: findDependency(id, forDependency, module, usedTypes, allowConstructorWithoutAnnotation)
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
        usedTypes: Set<IrType> = emptySet(),
        typeArgumentsMapping: Map<IrType?, IrType?>,
        providedParams: Map<DependencyId, Dependency>,
    ): List<ResolvedDependency>? {
        return valueParameters
            .mapNotNull { param ->
                resolveDependencyInternal(
                    DependencyId(typeArgumentsMapping[param.type] ?: param.type,
                        Annotations.getAnnotatedName(param).orEmpty()),
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
        usedTypes: Set<IrType> = emptySet(),
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
        module: IrClass,
        usedTypes: Set<IrType> = emptySet(),
        allowConstructorWithoutAnnotation: Boolean = false
    ): Dependency? {
        if (id.type in usedTypes) {
            forDependency.psiElement.error(
                "Recursive dependency in ${id.asErrorString()} needed to initialize ${forDependency.name} in module ${module.name.asString()}",
            )
            return null
        }

        val dependencyOptions = dependencyMap[id].orEmpty() - forDependency
        // check local and nested in groups as well as parameterless and parameterized
        return getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule == null && it.getRequiredParams().isEmpty() })
            ?: getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule == null && it.getRequiredParams().isNotEmpty() })
            ?: getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule != null && it.getRequiredParams().isEmpty() })
            ?: getDependencyFromGroup(forDependency, id, dependencyOptions.filter { it.fromNestedModule != null && it.getRequiredParams().isNotEmpty() })
            ?: getConstructorDependency(forDependency, id, allowConstructorWithoutAnnotation)
    }

    private fun getConstructorDependency(forDependency: Dependency, id: DependencyId, allowConstructorWithoutAnnotation: Boolean): Dependency? {
        val constructor = findConstructorInjector(id, allowConstructorWithoutAnnotation)
        if (constructor == null || !constructor.isVisible(module)) {
            forDependency.psiElement.error(
                "Can't resolve dependency ${id.asErrorString()} needed to initialize ${forDependency.name} in module ${module.name.asString()}",
            )
            return null
        }
        return Dependency.Constructor(constructor)
    }

    private fun getDependencyFromGroup(forDependency: Dependency, id: DependencyId, options: List<Dependency>): Dependency? {
        if (options.isNotEmpty()) {
            if (options.size > 1) {
                forDependency.psiElement.error(
                    "Multiple dependencies provided with type ${id.asErrorString()} in module ${module.name.asString()}",
                )
            }
            return options.first()
        }
        return null
    }

    private fun findConstructorInjector(id: DependencyId, allowConstructorWithoutAnnotation: Boolean): IrConstructor? {
        return id.type.getClass()
            ?.takeIf { id.name.isEmpty() }
            ?.let { clazz ->
                clazz.constructors.firstOrNull { it.hasAnnotation(Annotations.injectAnnotation) }
                    ?: clazz.takeIf { it.hasAnnotation(Annotations.injectAnnotation) || allowConstructorWithoutAnnotation }?.primaryConstructor
            }
    }
}