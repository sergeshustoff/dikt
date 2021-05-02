package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.DependencyId
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import java.util.*

sealed class Dependency {

    abstract val id: DependencyId

    abstract val irElement: IrDeclarationWithName

    abstract val name: String

    open val fromNestedModule: Dependency? = null

    fun nameWithNestedChain(): String? {
        var dependency: Dependency? = this
        val list = LinkedList<String>()
        while (dependency != null) {
            list.add(0, dependency.irElement.name.asString())
            dependency = dependency.fromNestedModule
        }
        return list.takeUnless { it.isEmpty() }?.joinToString(separator = ".")
    }

    fun isInNestedModulePath(path: Dependency): Boolean {
        var node: Dependency? = path
        while (node != null) {
            if (fromNestedModule == node) {
                return true
            }
            node = node.fromNestedModule
        }
        return false
    }

    abstract fun getRequiredParams(): List<IrValueParameter>

    data class Parameter(
        val parameter: IrValueParameter
    ) : Dependency() {
        override val id: DependencyId = DependencyId(parameter.type, Annotations.getAnnotatedName(parameter).orEmpty())
        override val irElement: IrDeclarationWithName = parameter
        override val name: String = parameter.name.asString()

        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
    }

    data class Property(
        val property: IrProperty,
        override val fromNestedModule: Dependency?,
        val returnType: IrType = property.getter!!.returnType
    ) : Dependency() {
        override val id: DependencyId = DependencyId(returnType, Annotations.getAnnotatedName(property).orEmpty())
        override val irElement: IrDeclarationWithName = property
        override val name: String = property.name.asString()

        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
    }

    data class Constructor(
        val constructor: IrConstructor,
    ) : Dependency() {
        override val id: DependencyId = DependencyId(constructor.returnType, "")
        override val irElement: IrDeclarationWithName = constructor
        override val name: String = constructor.name.asString()
        override fun getRequiredParams(): List<IrValueParameter> = constructor.valueParameters
    }

    data class Function(
        val function: IrFunction,
        override val fromNestedModule: Dependency?,
        val returnType: IrType = function.returnType
    ) : Dependency() {
        override val id: DependencyId = DependencyId(returnType, Annotations.getAnnotatedName(function).orEmpty())
        override val irElement: IrDeclarationWithName = function
        override val name: String = function.name.asString()
        override fun getRequiredParams(): List<IrValueParameter> = function.valueParameters
    }
}