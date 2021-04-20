package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecursiveDependencyTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `fail on recursive properties dependency`() {
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
            class Dependency(val injectable: Injectable)

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
                @ByDi fun dependency(): Dependency
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency in injectable in module MyModule")
        Truth.assertThat(result.messages).contains("Recursive dependency in dependency in module MyModule")
    }

    @Test
    fun `fail on recursive dependency in constructor`() {
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
            class Injectable(val injectable: Injectable)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency in dev.shustoff.dikt.compiler.Injectable needed to initialize injectable in module MyModule")
    }

    @Test
    fun `fail on recursive dependency in function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Inject
            import dev.shustoff.dikt.Module

            class Injectable(val injectable: Injectable)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
                fun injectable(injectable: Injectable) = Injectable(injectable)
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency in dev.shustoff.dikt.compiler.Injectable needed to initialize injectable in module MyModule")

        val result2 = compile(
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
                fun dependency(dependency: Dependency) = Dependency()
            }
            """
            )
        )
        Truth.assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result2.messages).contains("Recursive dependency in dev.shustoff.dikt.compiler.Dependency needed to initialize injectable in module MyModule")
    }

    @Test
    fun `fail on recursive dependency without di`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            @Inject
            class Dependency(val injectable: Injectable)

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                val injectable: Injectable by lazy { Injectable(dependency) }
                val dependency: Dependency = Dependency(injectable)
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency in injectable in module MyModule")
        Truth.assertThat(result.messages).contains("Recursive dependency in dependency in module MyModule")
    }

    @Test
    fun `fail on recursive dependency in methods`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject

            class Dependency1()
            class Dependency2()

            @Inject
            class Injectable(val dependency: Dependency1)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
                
                fun provideDependency1(): Dependency1 {
                    provideDependency2()
                    return Dependency1()
                }

                fun provideDependency2(): Dependency2 {
                    provideDependency1()
                    return Dependency2()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency in provideDependency1 in module MyModule")
        Truth.assertThat(result.messages).contains("Recursive dependency in provideDependency2 in module MyModule")
    }

    @Test
    fun `fail on recursive dependency between method and property`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.ByDi
            import dev.shustoff.dikt.Module
            import dev.shustoff.dikt.Inject


            class Injectable()

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable
                
                fun provideInjectable(): Injectable {
                    return injectable()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency in injectable in module MyModule")
        Truth.assertThat(result.messages).contains("Recursive dependency in provideInjectable in module MyModule")
    }

}