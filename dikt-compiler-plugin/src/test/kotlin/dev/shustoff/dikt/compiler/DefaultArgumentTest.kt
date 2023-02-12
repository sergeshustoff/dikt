@file:OptIn(ExperimentalCompilerApi::class)
package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.shustoff.dikt.compiler.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultArgumentTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `allow default arguments in injected constructors`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String = "default")

            @InjectByConstructors(TestObject::class)
            class MyModule {
                fun injectable(dependency: String): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow default arguments in generated methods`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String)

            @InjectByConstructors(TestObject::class)
            class MyModule {
                fun injectable(dependency: String = "default"): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `use default value if not provided from dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(
                val dependency: String = "default",
                val index: Int
            )

            @InjectByConstructors(TestObject::class)
            class MyModule {
                fun injectable(index: Int): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}