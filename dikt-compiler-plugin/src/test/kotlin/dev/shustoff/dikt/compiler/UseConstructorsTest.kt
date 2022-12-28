package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@Ignore
class UseConstructorsTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `allow constructor calls defined in containing class`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.UseConstructors

            class Dependency

            class Injectable(val dependency: Dependency)

            @UseConstructors(Dependency::class)
            class MyModule {
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow constructor calls defined in function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.UseConstructors

            class Dependency

            class Injectable(val dependency: Dependency)

            class MyModule {

                @UseConstructors(Dependency::class)
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow constructor calls defined in file`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            @file:UseConstructors(Dependency::class)
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.UseConstructors

            class Dependency

            class Injectable(val dependency: Dependency)

            class MyModule {

                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}