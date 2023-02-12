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

class CreatorTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `compile with dependency in constructor`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency

            class TestObject(val dependency: Dependency)

            class MyModule(val dependency: Dependency) {
                @Create fun injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `compile for injection with empty constructor`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class TestObject

            class MyModule {
                @Create fun injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `fail when dependency is missing`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create

            class Dependency()
            
            class TestObject(val dependency: Dependency)

            class MyModule {
                @Create fun injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Can't resolve dependency dev.shustoff.dikt.compiler.Dependency")
    }

    @Test
    fun `function may use dependency from params`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class TestObject(val dependency: Dependency)

            class MyModule {
                @Create fun injectable(dependency: Dependency): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `can compile cached injectable`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject

            class MyModule {
                @CreateSingle fun injectable(): TestObject
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

            class MyModule {
                @CreateSingle fun injectable(name: String): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("@CreateSingle functions can't have parameters")
    }

    @Test
    fun `fail on abstract create function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val name: String)

            interface MyModule {
                @Create fun injectable(name: String): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Only final functions can have generated body")
    }

    @Test
    fun `support global functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val name: String)

            @Create fun injectable(name: String): TestObject
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `support extension functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject(val name: String)

            @Create fun String.injectable(): TestObject
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `support extension functions in module with nested modules`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency()
            class TestObject(val name: String, val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            class Module(@ProvidesMembers val nestedModule: NestedModule) {
                @Create fun String.injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
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

            @CreateSingle fun String.injectable(): TestObject
            """
            )
        )

        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Function 'injectable' must have a body")
    }
}