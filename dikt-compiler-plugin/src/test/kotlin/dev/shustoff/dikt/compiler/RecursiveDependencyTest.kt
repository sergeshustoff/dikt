package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.shustoff.dikt.compiler.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCompilerApi::class)
class RecursiveDependencyTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `fail on recursive dependency in constructors`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency(val injectable: Injectable)

            class Injectable(val dependency: Dependency)

            @InjectByConstructors(Injectable::class, Dependency::class)
            class MyModule {
                fun injectable(): Injectable = resolve()
                fun dependency(): Dependency = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency")
    }

    @Test
    fun `fail on recursive dependency in constructor`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable(val injectable: Injectable)

            @InjectByConstructors(Injectable::class)
            class MyModule {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency: dev.shustoff.dikt.compiler.Injectable")
    }

    @Test
    fun `fail on recursive dependency in function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency()
            
            class Injectable(val dependency: Dependency)

            @InjectByConstructors(Injectable::class)
            class MyModule {
                fun injectable(): Injectable = resolve()
                fun dependency(dependency: Dependency) = Dependency()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency: dev.shustoff.dikt.compiler.Injectable -> dev.shustoff.dikt.compiler.Dependency")
    }

    @Test
    fun `fail on recursive dependency in several functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency1()
            class Dependency2()

            class Injectable(val dependency: Dependency1)

            @InjectByConstructors(Injectable::class)
            class MyModule {
                fun injectable(): Injectable = resolve()
                
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
        Truth.assertThat(result.messages).contains("Recursive dependency detected")
        Truth.assertThat(result.messages).contains("Recursive dependency detected")
    }

    @Test
    fun `fail on recursive dependency between between di and normal functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable()

            class MyModule {
                fun injectable(): Injectable = resolve()
                
                fun provideInjectable(): Injectable {
                    return injectable()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency detected")
    }

    @Test
    fun `fail on recursive dependency between between di and normal global functions`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable()

            fun injectable(): Injectable = resolve()
            
            fun provideInjectable(): Injectable {
                return injectable()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency detected")
    }

    @Test
    fun `ignore recursion if di is not involved`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable()
            class Injectable1()

            @InjectByConstructors(Injectable1::class)
            fun injectable1(): Injectable1 = resolve()

            fun injectable(): Injectable = provideInjectable()
            
            fun provideInjectable(): Injectable {
                return injectable()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `ignore recursion in module if di is not involved`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable()
            class Injectable1()

            @InjectByConstructors(Injectable1::class)
            class Module {
                fun injectable1(): Injectable1 = resolve()
    
                fun injectable(): Injectable = provideInjectable()
                
                fun provideInjectable(): Injectable {
                    return injectable()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

}