package dev.shustoff.dikt.core

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor


fun IrSimpleFunction.isVisible(
    module: IrClass
) = visibility.isVisible(null, toIrBasedDescriptor(), module.toIrBasedDescriptor())

fun IrProperty.isVisible(
    module: IrClass
) = visibility.isVisible(null, toIrBasedDescriptor(), module.toIrBasedDescriptor())


fun IrConstructor.isVisible(
    module: IrClass
) = visibility.isVisible(null, toIrBasedDescriptor(), module.toIrBasedDescriptor())