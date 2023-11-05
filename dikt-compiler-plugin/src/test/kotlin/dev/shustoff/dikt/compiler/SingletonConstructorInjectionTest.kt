package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@Ignore
@OptIn(ExperimentalCompilerApi::class)
class SingletonConstructorInjectionTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()


    @Test
    fun `can compile for singleton injectable`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject

            @InjectSingleByConstructors(TestObject::class)
            class MyModule {
                fun injectable(): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `fail on parameters in cached injectable`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val name: String)

            @InjectSingleByConstructors(TestObject::class)
            class MyModule {
                fun injectable(name: String): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Can't resolve dependency kotlin.String")
    }

    @Test
    fun `fail on cached extension functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val name: String)

            @InjectSingleByConstructors(TestObject::class)
            fun String.injectable(): TestObject = resolve()
            """
            )
        )

        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("This annotation is not applicable to target 'top level function'")
    }
}