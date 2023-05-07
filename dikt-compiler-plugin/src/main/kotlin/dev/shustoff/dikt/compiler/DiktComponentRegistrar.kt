package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.incrementalHelper
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class DiktComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val errorCollector = errorCollector(configuration)
        val incrementalCache = incrementalHelper(configuration)
        IrGenerationExtension.registerExtension(DiktIrGenerationExtension(errorCollector, incrementalCache))
    }
}