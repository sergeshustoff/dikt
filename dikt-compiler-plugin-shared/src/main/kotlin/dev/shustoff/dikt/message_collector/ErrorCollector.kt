package dev.shustoff.dikt.message_collector

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

interface ErrorCollector {
    fun PsiElement?.error(text: String)
    fun error(text: String)
}

private class ErrorCollectorImpl(
    private val messageCollector: MessageCollector
): ErrorCollector {
    override fun error(text: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, text, null)
    }

    override fun PsiElement?.error(text: String) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            text,
            MessageUtil.psiElementToMessageLocation(this)
        )
    }
}

fun errorCollector(messageCollector: MessageCollector): ErrorCollector = ErrorCollectorImpl(messageCollector)