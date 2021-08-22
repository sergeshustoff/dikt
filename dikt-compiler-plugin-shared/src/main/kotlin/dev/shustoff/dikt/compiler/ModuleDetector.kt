package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class ModuleDetector() : IrElementVisitor<Unit, MutableSet<IrClass>> {
    override fun visitElement(element: IrElement, data: MutableSet<IrClass>) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: MutableSet<IrClass>) {
        if (Annotations.isModule(declaration)) {
            data.add(declaration)
        }
        super.visitClass(declaration, data)
    }
}