package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.incrementalHelper
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor

class DiktComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val errorCollector = errorCollector(configuration)
        val incrementalCache = incrementalHelper(configuration)
        StorageComponentContainerContributor.registerExtension(project, DiktStorageComponentContainerContributor())
        IrGenerationExtension.registerExtension(project, DiktIrGenerationExtension(errorCollector, incrementalCache))
    }
}