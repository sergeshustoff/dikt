package dev.shustoff.dikt.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File

class DiktGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("dikt", DiktGradleExtension::class.java)
        target.configurations.filter { it.name.endsWith("implementation", ignoreCase = true) }.forEach {
            target.dependencies.add(it.name, target.dependencies.create("com.github.sergeshustoff.dikt:dikt-runtime:$version"))
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return kotlinCompilation.target.project.provider {
            listOf(
                SubpluginOption("cachesDir", File(project.project.buildDir, "diktCaches/${kotlinCompilation.compilationName}").path),
            )
        }
    }

    override fun getCompilerPluginId(): String = "com.github.sergeshustoff.dikt"

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact(
            groupId = "com.github.sergeshustoff.dikt",
            artifactId = "dikt-compiler-plugin",
            version = version
        )
    }

    override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.github.sergeshustoff.dikt",
        artifactId = "dikt-compiler-plugin-native",
        version = version
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.hasPlugin(DiktGradlePlugin::class.java)
    }

    companion object {
        private const val version = "1.0.0-alpha3"
    }
}

open class DiktGradleExtension