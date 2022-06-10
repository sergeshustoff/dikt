package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.parentClassOrNull

object Utils {
    fun getParentClasses(function: IrDeclaration): List<IrClass> {
        val parent = function.parentClassOrNull ?: return emptyList()
        val result = mutableListOf<IrClass>(parent)
        while (true) {
            result.add(result.lastOrNull()?.parentClassOrNull ?: return result)
        }
    }
}