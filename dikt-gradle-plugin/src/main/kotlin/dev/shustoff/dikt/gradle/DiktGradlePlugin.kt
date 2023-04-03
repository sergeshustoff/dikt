package dev.shustoff.dikt.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class DiktGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("dikt", DiktGradleExtension::class.java)
        target.afterEvaluate { project ->
            project.configurations.filter { it.name.endsWith("implementation", ignoreCase = true) }.forEach {
                project.dependencies.add(it.name, project.dependencies.create("io.github.sergeshustoff.dikt:dikt-annotations:$version"))
            }
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

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.hasPlugin(DiktGradlePlugin::class.java)
    }

    companion object {
        private const val version = "1.0.4"
    }
}

open class DiktGradleExtension