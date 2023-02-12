@file:OptIn(ExperimentalCompilerApi::class)
package dev.shustoff.dikt.compiler.oldApi

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.shustoff.dikt.compiler.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
            import dev.shustoff.dikt.Create

            @JvmInline
            value class Id(val value: String)
            @JvmInline
            value class Title(val value: String)

            class TestObject(
                val id: Id,
                val title: Title
            )

            class MyModule(
                val id: Id,
                val title: Title
            ) {
                @Create fun injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

}