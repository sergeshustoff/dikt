package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NullabilityTest {
    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `nullable type considered a different type`() {
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
                val dependency: String
            )

            class MyModule(
                val dependency: String?
            ) : Module() {
                val injectable by factory<Injectable>()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Can't resolve dependency kotlin.String needed to initialize property injectable in module MyModule")
    }

    @Test
    fun `nullable type injected just as normal type`() {
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
                val dependency: String?
            )

            class MyModule(
                val dependency: String?
            ) : Module() {
                val injectable by factory<Injectable>()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}