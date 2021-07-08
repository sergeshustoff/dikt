package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SingletonInTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `provide dependency with @SingletoneIn annotation in module`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @SingletonIn(MyModule::class)
            class Dependency()

            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `don't provide dependency with @SingletoneIn annotation in other modules`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @SingletonIn(OtherModule::class)
            class Dependency @Inject constructor()

            class Injectable(val dependency: Dependency)

            @Module
            class OtherModule

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("MyModule.injectable: Can't provide singleton of type dev.shustoff.dikt.compiler.Dependency bound to module dev.shustoff.dikt.compiler.OtherModule")
    }

    @Test
    fun `don't duplicate dependency with @SingletoneIn annotation in module if already has @SingletonByDi`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @SingletonIn(MyModule::class)
            class Dependency()

            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @SingletonByDi fun dependency(): Dependency
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}