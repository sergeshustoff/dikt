package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class AnnotatedSingletonDetector : IrElementVisitor<Unit, MutableMap<IrType, MutableList<IrClass>>> {
    override fun visitElement(element: IrElement, data: MutableMap<IrType, MutableList<IrClass>>) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: MutableMap<IrType, MutableList<IrClass>>) {
        Annotations.getSingletonModule(declaration)?.also { module ->
            data.getOrPut(module) { mutableListOf() }.add(declaration)
        }
        super.visitClass(declaration, data)
    }
}