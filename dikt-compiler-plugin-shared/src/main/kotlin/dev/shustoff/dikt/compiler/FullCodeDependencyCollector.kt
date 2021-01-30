package dev.shustoff.dikt.compiler

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class FullCodeDependencyCollector(
    private val symbols: Map<IrSymbol, IrDeclarationWithName>
) : IrElementVisitor<Unit, MutableSet<IrDeclarationWithName>> {
    override fun visitElement(element: IrElement, data: MutableSet<IrDeclarationWithName>) {
        element.acceptChildren(this, data)
    }

    override fun visitCall(expression: IrCall, data: MutableSet<IrDeclarationWithName>) {
        symbols[expression.symbol]?.also { data.add(it) }
        super.visitCall(expression, data)
    }
}