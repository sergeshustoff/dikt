package dev.shustoff.dikt.dependency

import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.types.IrType

data class DependencyId(
    val type: IrType,
) {

    fun asErrorString() = type.asString()
}