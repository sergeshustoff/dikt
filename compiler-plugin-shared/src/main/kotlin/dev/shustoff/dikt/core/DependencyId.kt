package dev.shustoff.dikt.core

import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.types.IrType

data class DependencyId(
    val type: IrType,
    val name: String = ""
) {

    fun asErrorString() = if (name.isEmpty()) {
        type.asString()
    } else {
        "${type.asString()} (with name $name)"
    }
}