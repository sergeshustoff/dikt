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
        Truth.assertThat(result.messages).contains("MyModule.injectable: Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.dependency: Recursive dependency detected")
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
        Truth.assertThat(result.messages).contains("MyModule.injectable: Recursive dependency: dev.shustoff.dikt.compiler.Injectable")
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
        Truth.assertThat(result.messages).contains("MyModule.injectable: Recursive dependency: dev.shustoff.dikt.compiler.Injectable")

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
        Truth.assertThat(result2.messages).contains("MyModule.injectable: Recursive dependency: dev.shustoff.dikt.compiler.Injectable -> dev.shustoff.dikt.compiler.Dependency")
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
        Truth.assertThat(result.messages).contains("MyModule.injectable: Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.dependency: Recursive dependency detected")
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
        Truth.assertThat(result.messages).contains("MyModule.provideDependency1: Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.provideDependency2: Recursive dependency detected")
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
        Truth.assertThat(result.messages).contains("MyModule.injectable: Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.provideInjectable: Recursive dependency detected")
    }

}