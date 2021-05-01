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
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.Module

            class Dependency

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable

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
            import dev.shustoff.dikt.*

            class Dependency

            @Inject
            class Injectable(@InjectNamed("1") val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable

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
            import dev.shustoff.dikt.*

            class Dependency(val name: String)

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule(
                @Named("1")
                val dependency1: String,
                @Named("2")
                val dependency2: String
            ) {
                @ByDi fun injectable(): Injectable

                private fun provideInjectable(@InjectNamed("2") dependency: String): Dependency {
                    return Dependency(dependency)
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    //TODO: handle this scenario
    @Ignore
    @Test
    fun `function may provide dependency for function within same module`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @Inject
            class Dependency()

            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable

                private fun provideInjectable(dependency: Dependency): Injectable {
                    return Injectable(dependency)
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `function may provide dependency from another module`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable()

            class OtherModule {
                fun injectable() = Injectable()
            }

            @Module
            class MyModule(private val other: OtherModule) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `function may provide dependency with params`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class OtherModule(val dependency: Dependency)

            @Module
            class MyModule(private val other: OtherModule) {
                @ByDi fun injectable(dependency: Dependency): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `function may provide cached dependency`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            @Inject
            class Injectable()

            @Module
            class MyModule {
                @Cached
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}