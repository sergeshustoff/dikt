package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.DependencyId
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrType

sealed class Dependency {

    abstract val id: DependencyId

    abstract fun getRequiredParams(): List<IrValueParameter>

    class Property(
        val property: IrProperty,
        val fromNestedModule: Property?
    ) : Dependency() {
        override val id: DependencyId = DependencyId(property.getter!!.returnType, Annotations.getAnnotatedName(property).orEmpty())
        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
    }

    class Constructor(
        val constructor: IrConstructor,
    ) : Dependency() {
        override val id: DependencyId = DependencyId(constructor.returnType, "")
        override fun getRequiredParams(): List<IrValueParameter> = constructor.valueParameters
    }

    class Function(
        val function: IrFunction,
        val fromNestedModule: Property?
    ) : Dependency() {
        override val id: DependencyId = DependencyId(function.returnType, Annotations.getAnnotatedName(function).orEmpty())
        override fun getRequiredParams(): List<IrValueParameter> = function.valueParameters
    }
}