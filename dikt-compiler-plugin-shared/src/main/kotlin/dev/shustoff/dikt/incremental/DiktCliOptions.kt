package dev.shustoff.dikt.incremental

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

enum class KspCliOption(
    override val optionName: String,
    override val valueDescription: String,
    override val description: String,
    override val required: Boolean = false,
    override val allowMultipleOccurrences: Boolean = false
) : AbstractCliOption {
    CACHES_DIR_OPTION(
        "cachesDir",
        "<cachesDir>",
        "Dir of caches",
        false
    );
}

val DIKT_CACHE = CompilerConfigurationKey.create<File>("cachesDir")