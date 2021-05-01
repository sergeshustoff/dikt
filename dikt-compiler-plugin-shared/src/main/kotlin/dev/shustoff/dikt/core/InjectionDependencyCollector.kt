package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import java.util.*

class InjectionDependencyCollector(
    private val errorCollector: ErrorCollector
) {
    fun collectDependencies(
        module: IrClass
    ): ModuleDependencies {
        val fullDependencyMap: MutableMap<DependencyId, MutableList<Dependency>> = mutableMapOf()
        module.properties
            .forEach {
                val dependency = Dependency.Property(it, null)
                fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
            }

        module.functions.forEach {
            createFunctionDependency(it)?.let { dependency ->
                fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
            }
        }

        val modules = LinkedList(fullDependencyMap.values.flatten()
            .mapNotNull {
                getModuleClassDescriptor(it)
                    ?.let { classDescriptor -> it to classDescriptor }
            }
            .toList()
        )

        while (modules.isNotEmpty()) {
            val (nestedModule, classDescriptor) = modules.pop()
            val dependencies = classDescriptor.properties
                .filter { it.isVisible(module) }
                .map { Dependency.Property(it, nestedModule) } +
                    classDescriptor.functions
                        .filter { it.isVisible(module) }
                        .mapNotNull { createFunctionDependency(it, nestedModule) }

            val withoutDuplicates = dependencies.filter { fullDependencyMap[it.id]?.any { it.fromNestedModule != null } != true }

            withoutDuplicates.forEach { dependency ->
                fullDependencyMap.getOrPut(dependency.id) { mutableListOf() }.add(dependency)
            }

            modules.addAll(
                withoutDuplicates.mapNotNull {
                    getModuleClassDescriptor(it)
                        ?.let { classDescriptor -> it to classDescriptor }
                }.toList()
            )
        }

        return ModuleDependencies(
            errorCollector,
            module,
            fullDependencyMap
        )
    }

    private fun createFunctionDependency(it: IrSimpleFunction, nestedModule: Dependency? = null): Dependency.Function? {
        if (!isDependencyFunction(it)) return null
        return Dependency.Function(it, nestedModule)
    }

    private fun isDependencyFunction(it: IrSimpleFunction) =
        !it.isFakeOverride && !it.isOperator && !it.isSuspend && !it.isInfix && !it.returnType.isUnit() && !it.returnType.isNothing()

    private fun getModuleClassDescriptor(dependency: Dependency) =
        dependency.id.type.getClass()?.takeIf { Annotations.isModule(it) }
}