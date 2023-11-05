package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCompilerApi::class)
class ResolveCallLocationsTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `can't call resolve in property initializer`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String = "default")

            @InjectByConstructors(TestObject::class)
            class MyModule(val dependency: String) {
                val injectable: TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Dependency can only be resolved inside functions and getters")
    }

    @Test
    fun `can't call resolve in constructor`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String = "default")

            @InjectByConstructors(TestObject::class)
            class MyModule(val dependency: String) {
                init {
                    val injectable = resolve<TestObject>()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Dependency can only be resolved inside functions and getters")
    }

    @Test
    fun `can't call resolve in property getter`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String = "default")

            @InjectByConstructors(TestObject::class)
            class MyModule(val dependency: String) {
                val injectable: TestObject get() = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Dependency can only be resolved inside functions and getters")
    }

    @Test
    fun `can't call resolve in property lazy delegate initializer`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String = "default")

            @InjectByConstructors(TestObject::class)
            class MyModule(val dependency: String) {
                val injectable: TestObject by lazy { resolve() }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Dependency can only be resolved inside functions and getters")
    }

    @Test
    fun `can call resolve when initializing variable inside function body`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val dependency: String)

            @InjectByConstructors(TestObject::class)
            class MyModule(val dependency: String) {
                fun processTestObject() {
                    val testObject = resolve<TestObject>()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}