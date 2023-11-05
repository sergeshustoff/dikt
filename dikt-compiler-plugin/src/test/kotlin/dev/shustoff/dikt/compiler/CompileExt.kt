package dev.shustoff.dikt.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
fun compile(root: File, vararg sourceFiles: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
        workingDir = root
        compilerPluginRegistrars = listOf(DiktComponentRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
    }.compile()
}