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

            class Dependency(val injectable: TestObject)

            class TestObject(val dependency: Dependency)

            @InjectByConstructors(TestObject::class, Dependency::class)
            class MyModule {
                fun injectable(): TestObject = resolve()
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

            class TestObject(val injectable: TestObject)

            @InjectByConstructors(TestObject::class)
            class MyModule {
                fun injectable(): TestObject = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency: dev.shustoff.dikt.compiler.TestObject")
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
            
            class TestObject(val dependency: Dependency)

            @InjectByConstructors(TestObject::class)
            class MyModule {
                fun injectable(): TestObject = resolve()
                fun dependency(dependency: Dependency) = Dependency()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency: dev.shustoff.dikt.compiler.TestObject -> dev.shustoff.dikt.compiler.Dependency")
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

            class TestObject(val dependency: Dependency1)

            @InjectByConstructors(TestObject::class)
            class MyModule {
                fun injectable(): TestObject = resolve()
                
                fun provideDependency1(dependency: Dependency2): Dependency1 {
                    return Dependency1()
                }

                fun provideDependency2(dependency: Dependency1): Dependency2 {
                    return Dependency2()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency: dev.shustoff.dikt.compiler.TestObject -> dev.shustoff.dikt.compiler.Dependency1 -> dev.shustoff.dikt.compiler.Dependency2")
    }

    @Test
    fun `fail on recursive dependency in provider function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class TestObject()

            class MyModule {
                fun injectable(): TestObject = resolve()
                
                fun provideTestObject(injectable: TestObject): TestObject {
                    return injectable
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Recursive dependency: dev.shustoff.dikt.compiler.TestObject")
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

            class TestObject()
            class TestObject1()

            @InjectByConstructors(TestObject1::class)
            fun injectable1(): TestObject1 = resolve()

            fun injectable(): TestObject = provideTestObject()
            
            fun provideTestObject(): TestObject {
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

            class TestObject()
            class TestObject1()

            @InjectByConstructors(TestObject1::class)
            class Module {
                fun injectable1(): TestObject1 = resolve()
    
                fun injectable(): TestObject = provideTestObject()
                
                fun provideTestObject(): TestObject {
                    return injectable()
                }
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `recursive dependency between functions is detected correctly`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Injectable1(val dependency: Injectable2)
            class Injectable2(val dependency: Injectable1)

            class MyModule() {
            
                @InjectByConstructors(Injectable1::class)
                fun injectable1(): Injectable1 = resolve()

                @InjectByConstructors(Injectable2::class)
                fun injectable2(): Injectable2 = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("injectable1: Recursive dependency detected")
        Truth.assertThat(result.messages).contains("injectable1: Recursive dependency detected")
        Truth.assertThat(result.messages).contains("MyModule: Recursive dependency detected: injectable1, injectable2")
    }
}