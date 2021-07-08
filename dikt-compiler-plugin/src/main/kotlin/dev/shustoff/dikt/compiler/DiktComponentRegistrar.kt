package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.incrementalCache
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor

class DiktComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val errorCollector = errorCollector(configuration)
        val incrementalCache = incrementalCache(configuration)
        val useIr = configuration.get(JVMConfigurationKeys.IR, true) //TODO: find how to detect ir in js

        if (!useIr) {
            errorCollector.error("Dikt plugin requires IR")
        }
        StorageComponentContainerContributor.registerExtension(project, DiktStorageComponentContainerContributor())
        IrGenerationExtension.registerExtension(project, DiktIrGenerationExtension(errorCollector, incrementalCache))
    }
}