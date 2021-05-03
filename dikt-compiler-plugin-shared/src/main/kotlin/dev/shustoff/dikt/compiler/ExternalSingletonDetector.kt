package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class ExternalSingletonDetector(
    private val errorCollector: ErrorCollector,
) : IrElementVisitor<Unit, MutableMap<IrType, MutableList<IrClass>>>, ErrorCollector by errorCollector {
    override fun visitElement(element: IrElement, data: MutableMap<IrType, MutableList<IrClass>>) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: MutableMap<IrType, MutableList<IrClass>>) {
        Annotations.getSingletonModule(declaration)?.also { module ->
            val moduleClass = module.getClass()
            if (moduleClass == null || !Annotations.isModule(moduleClass)) {
                declaration.error("Singleton can be provided only in class with @Module annotation")
            } else {
                data.getOrPut(module) { mutableListOf() }.add(declaration)
            }
        }
        super.visitClass(declaration, data)
    }
}