package dev.shustoff.dikt.message_collector

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fileEntry

interface ErrorCollector {
    fun IrDeclarationWithName?.info(text: String)
    fun IrDeclarationWithName?.error(text: String)
    fun info(text: String)
    fun error(text: String)
}

private class ErrorCollectorImpl(
    private val messageCollector: MessageCollector
): ErrorCollector {
    override fun IrDeclarationWithName?.error(text: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, text, location())
    }

    override fun error(text: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, text)
    }

    override fun IrDeclarationWithName?.info(text: String) {
        messageCollector.report(CompilerMessageSeverity.WARNING, text, location())
    }

    override fun info(text: String) {
        messageCollector.report(CompilerMessageSeverity.WARNING, text)
    }

    private fun IrDeclarationWithName?.location() = this?.let {
        CompilerMessageLocation.create(it.file.path,
            it.fileEntry.getLineNumber(it.startOffset),
            it.fileEntry.getColumnNumber(it.startOffset),
            null
        )
    }
}

fun errorCollector(
    configuration: CompilerConfiguration
): ErrorCollector = ErrorCollectorImpl(
    configuration.get(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        MessageCollector.NONE
    )
)

fun errorCollector(messageCollector: MessageCollector): ErrorCollector = ErrorCollectorImpl(messageCollector)