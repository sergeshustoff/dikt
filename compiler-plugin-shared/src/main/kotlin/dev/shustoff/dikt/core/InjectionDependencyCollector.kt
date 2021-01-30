package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import java.util.*

class InjectionDependencyCollector(
    private val moduleDeclarations: ModuleDeclarations,
    private val errorCollector: ErrorCollector
) {
    fun collectDependencies(
        module: IrClass
    ): ModuleDependencies {
        val fullDependencyList: MutableList<Dependency> = module.properties
            .map { Dependency.Property(it, null) }
            .toMutableList()

        fullDependencyList.addAll(
            module.functions.mapNotNull { createFunctionDependency(it) }
        )

        val modules = LinkedList(fullDependencyList
            .mapNotNull {
                if (it is Dependency.Property) {
                    getModuleClassDescriptor(it.property)
                        ?.let { classDescriptor -> it to classDescriptor }
                } else {
                    null
                }
            }
            .toList()
        )

        while (modules.isNotEmpty()) {
            val (nestedModule, classDescriptor) = modules.pop()
            val properties = classDescriptor.properties
                .filter { it.isVisible(module) }
                .map { Dependency.Property(it, nestedModule) }

            val functions = classDescriptor.functions
                .filter { it.isVisible(module) }
                .mapNotNull { createFunctionDependency(it, nestedModule) }

            fullDependencyList.addAll(properties)
            fullDependencyList.addAll(functions)

            modules.addAll(properties.mapNotNull {
                getModuleClassDescriptor(it.property)
                    ?.let { classDescriptor -> it to classDescriptor }
            })
        }

        return ModuleDependencies(
            errorCollector,
            module,
            fullDependencyList.groupBy { it.id }
        )
    }

    private fun createFunctionDependency(it: IrSimpleFunction, nestedModule: Dependency.Property? = null): Dependency.Function? {
        if (!isDependencyFunction(it)) return null
        return Dependency.Function(it, nestedModule)
    }

    private fun isDependencyFunction(it: IrSimpleFunction) =
        !it.isFakeOverride && !it.isOperator && !it.isSuspend && !it.isInfix && !it.returnType.isUnit() && !it.returnType.isNothing()

    private fun getModuleClassDescriptor(property: IrProperty) =
        property.getter?.returnType?.getClass()?.takeIf { moduleDeclarations.isModule(it) }
}