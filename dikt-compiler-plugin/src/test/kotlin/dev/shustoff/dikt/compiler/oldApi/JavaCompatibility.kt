@file:OptIn(ExperimentalCompilerApi::class)
package dev.shustoff.dikt.compiler.oldApi

import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.shustoff.dikt.compiler.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JavaCompatibility {

    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `can resolve dependency from java files`() {
        val result = compile(
            folder.root,
            SourceFile.java(
                "TestObject.java",
                """
            package dev.shustoff.dikt.compiler;
            
            public class TestObject {
                public final String dependency;
                public TestObject(String dependency) {
                    this.dependency = dependency;
                }
                private TestObject() {
                    this.dependency = "";
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
            import dev.shustoff.dikt.UseModules

            @UseModules(OtherModule::class)
            class MyModule(val other: OtherModule, val param: Int) {
                @Create fun injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `fail to resolve dependency with multiple constructors from java files`() {
        val result = compile(
            folder.root,
            SourceFile.java(
                "TestObject.java",
                """
            package dev.shustoff.dikt.compiler;
            
            public class TestObject {
                public final String dependency;
                public TestObject(String dependency) {
                    this.dependency = dependency;
                }
                public TestObject() {
                    this.dependency = "";
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
            import dev.shustoff.dikt.UseModules

            @UseModules(OtherModule::class)
            class MyModule(val other: OtherModule, val param: Int) {
                @Create fun injectable(): TestObject
            }
            """
            )
        )
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Truth.assertThat(result.messages).contains("Multiple visible constructors found for dev.shustoff.dikt.compiler.TestObject")
    }
}