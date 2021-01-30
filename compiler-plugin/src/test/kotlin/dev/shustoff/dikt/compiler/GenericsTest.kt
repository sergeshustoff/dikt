package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenericsTest {
    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `injections with different generics are considered as different types`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            @Inject
            class Injectable(
                val strings: List<String>,
                val numbers: List<Int>
            )

            class MyModule : Module() {
                val injectable by factory<Injectable>()
                
                fun provideStrings(): List<String> {
                    return listOf()
                }

                fun provideNumbers(): List<Int> {
                    return listOf()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `generic property supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            @Inject
            class Injectable<R>(
                val value: R
            )

            class MyModule<T>(val value: T) : Module() {
                val injectable by factory<Injectable<T>>()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `generic function supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            class Injectable<T>(
                val value: T
            )

            class MyModule<T>(val value: T) : Module() {
                val injectable by factory<Injectable<T>>()
                
                fun provideInjectable(value: T): Injectable<T> = Injectable(value)
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}