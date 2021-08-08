package dev.shustoff.dikt.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModuleDependencyTest {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `compile with dependency in nested module`() {
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

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class NestedModule(val dependency: Dependency)

            @Module
            class MyModule(val nested: NestedModule) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `compile with dependency depp in nested module`() {
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

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class NestedModule2(val dependency: Dependency)

            @Module
            class NestedModule(val nested: NestedModule2)

            @Module
            class MyModule(val nested: NestedModule) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `fail with duplicated property dependencies`() {
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

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule(
                val dependency1: Dependency,
                val dependency2: Dependency
            ) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("MyModule.kt: (15, 22): Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: dependency1, dependency2")
    }

    @Test
    fun `fail with duplicated function dependencies`() {
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

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule {
                @ByDi fun injectable(): Injectable

                fun provide1() = Dependency()
                fun provide2() = Dependency()
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("MyModule.kt: (12, 10): Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: provide1, provide2")
    }

    @Test
    fun `fail with duplicated function dependencies in nested modules`() {
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

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class Module1 {
                fun dependency() = Dependency()
            }

            @Module
            class Module2 {
                fun dependency() = Dependency()            
            }

            @Module
            class MyModule(
                val module1: Module1,
                val module2: Module2
            ) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("MyModule.kt: (25, 10): Multiple dependencies provided with type dev.shustoff.dikt.compiler.Dependency: module1.dependency, module2.dependency")
    }

    @Test
    fun `compile with dependency`() {
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

            @Inject
            class Injectable(val dependency: Dependency)

            @Module
            class MyModule(val dependency: Dependency) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `module can be interface`() {
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

            @Inject
            class Injectable(val dependency: Dependency)
            
            @Module
            interface OtherModule {
                val dependency: Dependency
            }
    
            @Module
            class MyModule(val other: OtherModule) {
                @ByDi fun injectable(): Injectable
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `cant module can't have @ByDi methods`() {
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

            @Inject
            class Injectable(val dependency: Dependency)
            
            @Module
            interface Module {
                val dependency: Dependency
                @ByDi fun injectable(): Injectable            
            }
            """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    }
}