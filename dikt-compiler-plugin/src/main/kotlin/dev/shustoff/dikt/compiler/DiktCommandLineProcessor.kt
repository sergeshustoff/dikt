package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.incremental.DIKT_CACHE
import dev.shustoff.dikt.incremental.KspCliOption
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

class DiktCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.github.sergeshustoff.dikt"
    override val pluginOptions: Collection<AbstractCliOption> = KspCliOption.values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option !is KspCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        when (option) {
            KspCliOption.CACHES_DIR_OPTION -> configuration.put(DIKT_CACHE, File(value))
        }
    }
}