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
class ValueClassParamsTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `pass with 2 different typealias parameters for single type`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @JvmInline
            value class Id(val value: String)
            @JvmInline
            value class Title(val value: String)

            class TestObject(
                val id: Id,
                val title: Title
            )

            @InjectByConstructors(TestObject::class)
            class MyModule(
                val id: Id,
                val title: Title
            ) {
                fun injectable(): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

}