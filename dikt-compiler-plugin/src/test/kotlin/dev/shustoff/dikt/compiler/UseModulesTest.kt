package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @DiModule
            @UseModules(NestedModule::class)
            class MyModule(val nested: NestedModule) {
                @Create fun injectable(): Injectable
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
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @DiModule
            class MyModule(val nested: NestedModule) {
                @Create fun injectable(): Injectable
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
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @DiModule
            class MyModule() {
                @UseModules(NestedModule::class)
                @Create fun injectable(nested: NestedModule): Injectable
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
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule(val dependency: Dependency)

            @DiModule
            class MyModule(val nested: NestedModule) {
                @UseModules(NestedModule::class)
                @Create fun injectable(): Injectable
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
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)

            class NestedModule2(val dependency: Dependency)

            @DiModule
            @UseModules(NestedModule2::class)
            class NestedModule(val nested: NestedModule2)

            @DiModule
            @UseModules(NestedModule::class)
            class MyModule(val nested: NestedModule) {
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages)
            .contains("MyModule.kt: (18, 12): Can't resolve dependency dev.shustoff.dikt.compiler.Dependency")
    }

    @Test
    fun `fail with duplicated external dependencies`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)

            class Module1 {
                fun dependency() = Dependency()
            }

            class Module2 {
                fun dependency() = Dependency()            
            }

            @DiModule
            @UseModules(Module1::class, Module2::class)
            class MyModule(
                val module1: Module1,
                val module2: Module2
            ) {
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages)
            .contains("MyModule.kt: (23, 12): Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: module1.dependency, module2.dependency")
    }


    @Test
    fun `can get external dependency from interface`() {
        val result = compile(
            folder.root,
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            class Dependency

            class Injectable(val dependency: Dependency)
            
            interface OtherModule {
                val dependency: Dependency
            }
    
            @DiModule
            @UseModules(OtherModule::class)
            class MyModule(val other: OtherModule) {
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `can resolve dependency from java files`() {
        val result = compile(
            folder.root,
            SourceFile.java(
                "Injectable.java",
                """
            package dev.shustoff.dikt.compiler;
            
            public class Injectable {
                public final String dependency;
                public Injectable(String dependency) {
                    this.dependency = dependency;
                }
            }
            """
            ),
            SourceFile.java(
                "OtherModule.java",
                """
            package dev.shustoff.dikt.compiler;
            
            public interface OtherModule {
                public String dependency(int param);
            }
            """
            ),
            SourceFile.kotlin(
                "MyModule.kt",
                """
            package dev.shustoff.dikt.compiler
            import dev.shustoff.dikt.Create
            import dev.shustoff.dikt.DiModule
            import dev.shustoff.dikt.UseModules

            @DiModule
            @UseModules(OtherModule::class)
            class MyModule(val other: OtherModule, val param: Int) {
                @Create fun injectable(): Injectable
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}