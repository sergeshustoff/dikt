package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCompilerApi::class)
class InjectByConstructorTest {
    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `allow constructor calls for InjectByConstructor implementations`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency : InjectByConstructor

            class TestObject(val dependency: Dependency): InjectByConstructor

            class MyModule {
                fun injectable(): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `resolve InjectByConstructor from nested module if parameters for InjectByConstructor not available`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class TestObject(val dependency: Dependency): InjectByConstructor

            class NestedModule(private val dependency: Dependency) {
                fun injectable() = resolve<TestObject>()
            }

            class MyModule(@ProvidesMembers val module: NestedModule) {
                fun injectable(): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}