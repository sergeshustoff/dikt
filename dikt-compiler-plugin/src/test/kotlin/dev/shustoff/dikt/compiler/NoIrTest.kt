package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoIrTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `compiles with IR`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            @Inject
            class Injectable()

            class MyModule : Module() {
                val injectable: Injectable by factory()
            }
            """
            ),
            useIR = true
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `compilation fails without IR`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            @Inject
            class Injectable()

            class MyModule : Module() {
                val injectable: Injectable by factory()
            }
            """
            ),
            useIR = false
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Dikt plugin requires IR")
    }
}