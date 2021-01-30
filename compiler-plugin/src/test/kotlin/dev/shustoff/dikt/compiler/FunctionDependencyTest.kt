package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FunctionDependencyTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `allow functions for dependency resolution`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.Module

            class Dependency

            @Inject
            class Injectable(val dependency: Dependency)

            class MyModule : Module() {
                val injectable: Injectable by factory()

                private fun provideDependency(): Dependency {
                    return Dependency()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `functions may provide named dependency`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.InjectNamed
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Named

            class Dependency

            @Inject
            class Injectable(@InjectNamed("1") val dependency: Dependency)

            class MyModule : Module() {
                val injectable: Injectable by factory()

                @Named("1")
                private fun provideDependency1(): Dependency {
                    return Dependency()
                }

                @Named("2")
                private fun provideDependency2(): Dependency {
                    return Dependency()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `functions may use named dependency`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.InjectNamed
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Named

            class Dependency(val name: String)

            @Inject
            class Injectable(val dependency: Dependency)

            class MyModule(
                @Named("1")
                val dependency1: String,
                @Named("2")
                val dependency2: String
            ) : Module() {
                val injectable: Injectable by factory()

                private fun provideInjectable(@InjectNamed("2") dependency: String): Dependency {
                    return Dependency(dependency)
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `function may provide dependency for property within same module`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.InjectNamed
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Named

            class Injectable()

            class MyModule : Module() {
                val injectable: Injectable by factory()

                private fun provideInjectable(): Injectable {
                    return Injectable()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

}