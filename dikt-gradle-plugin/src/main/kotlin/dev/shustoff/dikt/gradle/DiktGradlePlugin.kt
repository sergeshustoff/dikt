package dev.shustoff.dikt.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class DiktGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("dikt", DiktGradleExtension::class.java)
        target.configurations.filter { it.name.endsWith("implementation", ignoreCase = true) }.forEach {
            target.dependencies.add(it.name, target.dependencies.create("io.github.sergeshustoff.dikt:dikt-runtime:$version"))
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = "io.github.sergeshustoff.dikt"

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact(
            groupId = "io.github.sergeshustoff.dikt",
            artifactId = "dikt-compiler-plugin",
            version = version
        )
    }

    override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.github.sergeshustoff.dikt",
        artifactId = "dikt-compiler-plugin-native",
        version = version
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.hasPlugin(DiktGradlePlugin::class.java)
    }

    companion object {
        private const val version = "1.0.0-alpha1"
    }
}

open class DiktGradleExtension