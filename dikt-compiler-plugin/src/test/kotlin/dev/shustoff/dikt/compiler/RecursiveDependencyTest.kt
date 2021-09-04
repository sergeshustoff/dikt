package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
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
            import dev.shustoff.dikt.DiModule

            class Dependency(val injectable: Injectable)

            class Injectable(val dependency: Dependency)

            @DiModule
            class MyModule {
                @ByDi fun injectable(): Injectable
                @ByDi fun dependency(): Dependency
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("MyModule.kt: (10, 10): Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.kt: (11, 10): Recursive dependency detected")
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
            import dev.shustoff.dikt.DiModule

            class Injectable(val injectable: Injectable)

            @DiModule
            class MyModule {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("MyModule.kt: (8, 10): Recursive dependency: dev.shustoff.dikt.compiler.Injectable")
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
            import dev.shustoff.dikt.DiModule

            class Dependency()
            
            class Injectable(val dependency: Dependency)

            @DiModule
            class MyModule {
                @ByDi fun injectable(): Injectable
                fun dependency(dependency: Dependency) = Dependency()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("MyModule.kt: (10, 10): Recursive dependency: dev.shustoff.dikt.compiler.Injectable -> dev.shustoff.dikt.compiler.Dependency")
    }

    @Test
    fun `fail on recursive dependency without di`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.DiModule

            class Dependency(val injectable: Injectable)

            class Injectable(val dependency: Dependency)

            @DiModule
            class MyModule {
                val injectable: Injectable by lazy { Injectable(dependency) }
                val dependency: Dependency = Dependency(injectable)
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("MyModule.kt: (9, 4): Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.kt: (10, 4): Recursive dependency detected")
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
            import dev.shustoff.dikt.DiModule

            class Dependency1()
            class Dependency2()

            class Injectable(val dependency: Dependency1)

            @DiModule
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
        Truth.assertThat(result.messages).contains("MyModule.kt: (13, 4): Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.kt: (18, 4): Recursive dependency detected")
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
            import dev.shustoff.dikt.DiModule


            class Injectable()

            @DiModule
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
        Truth.assertThat(result.messages).contains("MyModule.kt: (9, 10): Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule.kt: (11, 4): Recursive dependency detected")
    }

}