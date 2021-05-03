package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExtensionDependencyTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `injection by extension functions is supported`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency()

            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @SingletonByDi fun dependency(): Dependency
            }

            @ByDi fun MyModule.injectable(): Injectable
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
    @Test
    fun `singleton is not supported for extension functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @Inject
            class Dependency()

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @SingletonByDi fun dependency(): Dependency
            }

            @SingletonByDi fun MyModule.injectable(): Injectable
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    }
}