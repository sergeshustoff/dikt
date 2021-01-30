package dev.shustoff.dikt.compiler

import com.intellij.mock.MockProject
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class DiktNativeComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val messageCollector = configuration.get(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE
        )
        val errorCollector = errorCollector(messageCollector)
        IrGenerationExtension.registerExtension(project, DiktIrGenerationExtension(errorCollector))
    }
}