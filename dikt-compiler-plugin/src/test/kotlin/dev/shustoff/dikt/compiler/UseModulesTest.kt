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
class UseModulesTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `compile with external dependency in containing class`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @InjectByConstructors(Injectable::class)
            @UseModules(NestedModule::class)
            class MyModule(val nested: NestedModule) {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `compile with external dependency in file`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            @file:UseModules(NestedModule::class)
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @InjectByConstructors(Injectable::class)
            class MyModule(val nested: NestedModule) {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `compile with external dependency in function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @InjectByConstructors(Injectable::class)
            class MyModule() {
                @UseModules(NestedModule::class)
                fun injectable(nested: NestedModule): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `compile with external dependency in function, but provided in module`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @InjectByConstructors(Injectable::class)
            class MyModule(val nested: NestedModule) {
                @UseModules(NestedModule::class)
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `fail with indirect external dependency`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule2(val dependency: Dependency)

            @UseModules(NestedModule2::class)
            class NestedModule(val nested: NestedModule2)

            @InjectByConstructors(Injectable::class)
            @UseModules(NestedModule::class)
            class MyModule(val nested: NestedModule) {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages)
            .contains("Can't resolve dependency dev.shustoff.dikt.compiler.Dependency")
    }

    @Test
    fun `fail with duplicated external dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)

            class Module1 {
                fun dependency() = Dependency()
            }

            class Module2 {
                fun dependency() = Dependency()            
            }

            @InjectByConstructors(Injectable::class)
            @UseModules(Module1::class, Module2::class)
            class MyModule(
                val module1: Module1,
                val module2: Module2
            ) {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages)
            .contains("Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: module1.dependency, module2.dependency")
    }


    @Test
    fun `can get external dependency from interface`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)
            
            interface OtherModule {
                val dependency: Dependency
            }
    
            @InjectByConstructors(Injectable::class)
            @UseModules(OtherModule::class)
            class MyModule(val other: OtherModule) {
                fun injectable(): Injectable = resolve()
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow module as receiver in extension function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)
            
            interface OtherModule {
                val dependency: Dependency
            }
    
            @InjectByConstructors(Injectable::class)
            @UseModules(OtherModule::class)
            fun OtherModule.injectable(): Injectable = resolve()
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `allow module as parameter in extension function`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.*

            class Dependency

            class Injectable(val dependency: Dependency)
            
            interface OtherModule {
                val dependency: Dependency
            }
    
            @InjectByConstructors(Injectable::class)
            @UseModules(OtherModule::class)
            fun injectable(module: OtherModule): Injectable = resolve()
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}