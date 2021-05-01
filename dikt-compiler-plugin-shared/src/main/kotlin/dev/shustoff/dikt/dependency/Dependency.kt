package dev.shustoff.dikt.dependency

import dev.shustoff.dikt.compiler.psiElementSafe
import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.DependencyId
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

sealed class Dependency {

    abstract val id: DependencyId

    abstract val psiElement: PsiElement?

    abstract val name: String

    abstract fun getRequiredParams(): List<IrValueParameter>

    data class Parameter(
        val parameter: IrValueParameter
    ) : Dependency() {
        override val id: DependencyId = DependencyId(parameter.type, Annotations.getAnnotatedName(parameter).orEmpty())
        override val psiElement: PsiElement? = parameter.psiElementSafe
        override val name: String = parameter.name.asString()

        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
    }

    data class Property(
        val property: IrProperty,
        val fromNestedModule: Property?
    ) : Dependency() {
        override val id: DependencyId = DependencyId(property.getter!!.returnType, Annotations.getAnnotatedName(property).orEmpty())
        override val psiElement: PsiElement? = property.psiElementSafe
        override val name: String = property.name.asString()

        override fun getRequiredParams(): List<IrValueParameter> = emptyList()
    }

    data class Constructor(
        val constructor: IrConstructor,
    ) : Dependency() {
        override val id: DependencyId = DependencyId(constructor.returnType, "")
        override val psiElement: PsiElement? = constructor.psiElementSafe
        override val name: String = constructor.name.asString()
        override fun getRequiredParams(): List<IrValueParameter> = constructor.valueParameters
    }

    data class Function(
        val function: IrFunction,
        val fromNestedModule: Property?
    ) : Dependency() {
        override val id: DependencyId = DependencyId(function.returnType, Annotations.getAnnotatedName(function).orEmpty())
        override val psiElement: PsiElement? = function.psiElementSafe
        override val name: String = function.name.asString()
        override fun getRequiredParams(): List<IrValueParameter> = function.valueParameters
    }
}