package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys

class DiktComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val messageCollector = configuration.get(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE
        )
        val errorCollector = errorCollector(messageCollector)
        val useIr = configuration.get(JVMConfigurationKeys.IR, true) //TODO: find how to detect ir in js

        if (!useIr) {
            errorCollector.error("Dikt plugin requires IR")
        }
        IrGenerationExtension.registerExtension(project, DiktIrGenerationExtension(errorCollector))
    }
}