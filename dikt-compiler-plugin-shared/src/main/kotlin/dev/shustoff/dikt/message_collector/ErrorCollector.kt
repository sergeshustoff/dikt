package dev.shustoff.dikt.message_collector

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.util.getPackageFragment
import java.util.*

interface ErrorCollector {
    fun IrDeclarationWithName?.info(text: String)
    fun IrDeclarationWithName?.error(text: String)
    fun error(text: String)
}

private class ErrorCollectorImpl(
    private val messageCollector: MessageCollector
): ErrorCollector {
    override fun error(text: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, text, null)
    }

    override fun IrDeclarationWithName?.error(text: String) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            fullName()?.let { "$it: $text" } ?: text,
        )
    }

    override fun IrDeclarationWithName?.info(text: String) {
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            fullName()?.let { "$it: $text" } ?: text,
        )
    }

    private fun IrDeclarationWithName?.fullName(): String? {
        var node = this
        val nodes = LinkedList<String>()
        while (node != null) {
            nodes.add(0, node.name.asString())
            val parent = node.parent as? IrDeclarationWithName
            if (parent == null) {
                node.getPackageFragment()?.fqName?.asString()?.let { nodes.add(0, it) }
            }
            node = parent
        }

        return nodes.takeUnless { it.isEmpty() }?.joinToString(separator = ".")
    }
}

fun errorCollector(messageCollector: MessageCollector): ErrorCollector = ErrorCollectorImpl(messageCollector)