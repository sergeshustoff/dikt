package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.CollectAdditionalSourcesExtension
import org.jetbrains.kotlin.psi.KtFile

class DiktCollectAdditionalSourcesExtension(
    private val incrementalHelper: IncrementalHelper,
    private val errorCollector: ErrorCollector
) : CollectAdditionalSourcesExtension {
    override fun collectAdditionalSourcesAndUpdateConfiguration(
        knownSources: Collection<KtFile>,
        configuration: CompilerConfiguration,
        project: Project
    ): Collection<KtFile> {
        errorCollector.info("Dikt collecting sources: ${knownSources.map { it.name }}")
        return emptyList()
    }
}