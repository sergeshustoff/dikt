package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.jvm.codegen.psiElement
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class InvalidDiCallDetector(
    errorCollector: ErrorCollector,
    private val diFunctionSymbols: Set<IrSimpleFunctionSymbol>
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        if (expression.symbol in diFunctionSymbols && expression.getValueArgument(0) == null) {
            expression.psiElement.error("Invalid ${expression.symbol.owner.name.identifier} method call")
        }
        super.visitCall(expression)
    }
}