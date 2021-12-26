package dev.shustoff.dikt.dependency

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import java.util.*

sealed class ProvidedDependency {

    abstract val id: DependencyId

    abstract val irElement: IrDeclarationWithName

    open val fromNestedModule: ProvidedDependency? = null

    fun nameWithNestedChain(): String? {
        var dependency: ProvidedDependency? = this
        val list = LinkedList<String>()
        while (dependency != null) {
            list.add(0, dependency.irElement.name.asString())
            dependency = dependency.fromNestedModule
        }
        return list.takeUnless { it.isEmpty() }?.joinToString(separator = ".")
    }

    fun isInNestedModulePath(path: ProvidedDependency): Boolean {
        var node: ProvidedDependency? = path
        while (node != null) {
            if (fromNestedModule == node) {
                return true
            }
            node = node.fromNestedModule
        }
        return false
    }

    abstract fun getRequiredParams(): List<IrValueParameter>

    abstract fun getRequiredExtensionReceiver(): IrValueParameter?

    data class Parameter(
        val parameter: IrValueParameter
    ) : ProvidedDependency() {
        override val id: DependencyId = DependencyId(parameter.type)
        override val irElement: IrDeclarationWithName = parameter
        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
        override fun getRequiredExtensionReceiver(): IrValueParameter? = null
    }

    data class Property(
        val property: IrProperty,
        override val fromNestedModule: ProvidedDependency?,
        val returnType: IrType = property.getter!!.returnType
    ) : ProvidedDependency() {
        override val id: DependencyId = DependencyId(returnType)
        override val irElement: IrDeclarationWithName = property
        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
        override fun getRequiredExtensionReceiver(): IrValueParameter? = property.getter?.extensionReceiverParameter
    }

    data class Function(
        val function: IrFunction,
        override val fromNestedModule: ProvidedDependency?,
        val returnType: IrType = function.returnType
    ) : ProvidedDependency() {
        override val id: DependencyId = DependencyId(returnType)
        override val irElement: IrDeclarationWithName = function
        override fun getRequiredParams(): List<IrValueParameter> = function.valueParameters
        override fun getRequiredExtensionReceiver(): IrValueParameter? = function.extensionReceiverParameter
    }
}