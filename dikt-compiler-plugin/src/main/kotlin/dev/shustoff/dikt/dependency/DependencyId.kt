package dev.shustoff.dikt.dependency

import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName

data class DependencyId(
    val type: IrType,
) {

    fun asErrorString() = type.classFqName!!.asString()
}