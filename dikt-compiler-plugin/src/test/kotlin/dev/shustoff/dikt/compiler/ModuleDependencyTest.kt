package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModuleDependencyTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `compile with dependency in nested module`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            class Dependency

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class NestedModule(val dependency: Dependency)

            @Module
            class MyModule(val nested: NestedModule) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `fail with duplicated property dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            class Dependency

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule(
                val dependency1: Dependency,
                val dependency2: Dependency
            ) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency in module MyModule")
    }

    @Test
    fun `fail with duplicated function dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            class Dependency

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable

                fun provide1() = Dependency()
                fun provide2() = Dependency()
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency in module MyModule")
    }

    @Test
    fun `compile with dependency`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            class Dependency

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule(val dependency: Dependency) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}