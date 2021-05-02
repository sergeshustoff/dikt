package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConstructorInjectionsTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `compile with multiple dependencies marked with @Named`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable @Inject constructor(@InjectNamed("first") val dependency: Dependency)

            @Module
            class MyModule(
                @Named("first") val dependency1: Dependency,
                @Named("second") val dependency2: Dependency
            ) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow nested injections with inject annotation`() {
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

            class Injectable @Inject constructor(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
                fun dependency(): Dependency = Dependency()
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
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            @Inject
            class Injectable

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
    fun `don't require inject annotation for direct dependency constructors`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.Module

            @Inject
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
    fun `require inject annotation in nested dependency`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.Module

            class Dependency()
            
            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("MyModule.injectable: Can't resolve dependency dev.shustoff.dikt.compiler.Dependency")
    }
}