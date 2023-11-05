package dev.shustoff.dikt.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.shustoff.dikt.incremental.incrementalHelper
import dev.shustoff.dikt.message_collector.errorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
fun compile(root: File, vararg sourceFiles: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
        workingDir = root
        //TODO: uncomment when fixed
//        compilerPluginRegistrars = listOf(DiktComponentRegistrar())
        componentRegistrars = listOf(DiktComponentRegistrarLegacy())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
    }.compile()
}

@OptIn(ExperimentalCompilerApi::class)
class DiktComponentRegistrarLegacy : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val errorCollector = errorCollector(configuration)
        val incrementalCache = incrementalHelper(configuration)
        val useIr = configuration.get(JVMConfigurationKeys.IR, true)

        if (!useIr) {
            errorCollector.error("Dikt plugin requires IR")
        }
        StorageComponentContainerContributor.registerExtension(project, DiktStorageComponentContainerContributor())
        IrGenerationExtension.registerExtension(project, DiktIrGenerationExtension(errorCollector, incrementalCache))
    }
}