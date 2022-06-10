package dev.shustoff.dikt.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

fun compile(root: File, vararg sourceFiles: SourceFile, useIR: Boolean = true): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        workingDir = root
        compilerPlugins = listOf<ComponentRegistrar>(DiktComponentRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
        jvmTarget = JvmTarget.JVM_1_8.description
    }.compile()
}