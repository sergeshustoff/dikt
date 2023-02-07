package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.shustoff.dikt.compiler.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCompilerApi::class)
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
            import dev.shustoff.dikt.*

            class Injectable(
                val strings: List<String>,
                val numbers: List<Int>
            )

            @InjectByConstructors(Injectable::class)
            class MyModule {
                fun injectable(): Injectable = resolve()
                
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
    fun `generic injectable supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable<T>(
                val value: T
            )

            @InjectByConstructors(Injectable::class)
            class MyModule<T>(val value: T) {
                fun injectable(): Injectable<T> = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
    @Test
    fun `generic singleton supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable<T>(
                val value: T
            )

            @InjectSingleByConstructors(Injectable::class)
            class MyModule<T>(val value: T) {
                fun injectable(): Injectable<T> = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
    @Test
    fun `fail on generic singleton if can't cache in module singleton`() {
        //TODO: also add generic testing in sample
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable<T>(
                val value: T
            )

            @InjectSingleByConstructors(Injectable::class)
            class MyModule {
                fun <T> injectable(value: T): Injectable<T> = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Need message here")
    }

    @Test
    fun `generic modules supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable(
                val value: String
            )

            class GenericModule<T>(val value: T)

            @InjectByConstructors(Injectable::class)
            @UseModules(GenericModule::class)
            class MyModule(val module: GenericModule<String>) {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}