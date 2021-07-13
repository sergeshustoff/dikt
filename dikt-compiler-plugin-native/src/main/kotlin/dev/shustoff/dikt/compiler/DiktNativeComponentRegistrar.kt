package dev.shustoff.dikt.compiler

import com.intellij.mock.MockProject
import dev.shustoff.dikt.incremental.incrementalCache
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor

class DiktNativeComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val errorCollector = errorCollector(configuration)
        val incrementalCache = incrementalCache(configuration, errorCollector)
        StorageComponentContainerContributor.registerExtension(project, DiktStorageComponentContainerContributor())
        IrGenerationExtension.registerExtension(project, DiktIrGenerationExtension(errorCollector, incrementalCache))
    }
}