package dev.shustoff.dikt.dependency

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.IrType

sealed class ResolvedDependency {
    abstract val type: IrType

    data class Provided(
        val provided: ProvidedDependency,
        val nestedModulesChain: ResolvedDependency? = null,
        val params: List<ResolvedDependency> = emptyList(),
        val extensionParam: ResolvedDependency? = null,
    ) : ResolvedDependency() {
        override val type: IrType
            get() = provided.id.type
    }

    data class Constructor(
        override val type: IrType,
        val constructor: IrConstructor,
        val params: List<ResolvedDependency> = emptyList(),
        val isSingleton: Boolean = false
    ) : ResolvedDependency()

    data class ParameterDefaultValue(
        override val type: IrType,
        val defaultValue: IrExpressionBody
    ) : ResolvedDependency()
}