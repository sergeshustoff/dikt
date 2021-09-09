package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor

class VisibilityChecker private constructor(
    private val visibleFrom: () -> DeclarationDescriptor
) {

    constructor(clazz: IrClass) : this({ clazz.toIrBasedDescriptor() })

    constructor(function: IrFunction) : this({ function.toIrBasedDescriptor() })

    fun isVisible(target: IrConstructor): Boolean =
        target.visibility.isVisible(null, target.toIrBasedDescriptor(), visibleFrom())

    fun isVisible(target: IrProperty): Boolean =
        target.visibility.isVisible(null, target.toIrBasedDescriptor(), visibleFrom())

    fun isVisible(target: IrFunction): Boolean =
        target.visibility.isVisible(null, target.toIrBasedDescriptor(), visibleFrom())
}