package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.ModuleDependencies
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName
import java.io.File

class IncrementalCache(cacheDir: File) {
    // Something that had or has SingletonIn (added/changed/removed) -> consider target module changed
    // Something created by constructor (changed) -> recompile module that called the constructor (but not module's dependencies)
    // Module (changed) -> recompile all modules that depended on it in case it provides dependency to replace a constructor call

    val singletonsByModule = ClassToStringsListMap(File(cacheDir, "singletonsByModule"))
    val dependantModulesByModule = ClassToStringsListMap(File(cacheDir, "dependantModulesByModule"))
    val dependantModulesByInjectedConstructor = ClassToStringsListMap(File(cacheDir, "dependantModulesByInjectedConstructor"))

//    val modulesSnapshots = FileSnapshotMap(File(cacheDir, "modulesSnapshots"), FileToCanonicalPathConverter)

    fun flush() {
        dependantModulesByModule.flush(false)
        singletonsByModule.flush(false)
        dependantModulesByInjectedConstructor.flush(false)
    }

    fun getSingletons(
        declaration: IrClass,
        foundSingletons: List<IrClass>,
        pluginContext: IrPluginContext,
        errorCollector: ErrorCollector
    ): List<IrClass> {
        val cachedSingletons = singletonsByModule[declaration.kotlinFqName].mapNotNull {
            pluginContext.referenceClass(FqName(it))?.owner
                ?.takeIf {
                    val cachedSingletonModule = Annotations.getSingletonModule(it)
                    val matches = cachedSingletonModule == declaration.defaultType
                    errorCollector.info("Dikt detekted cached singleton module ${cachedSingletonModule?.classFqName?.asString()}, $matches")
                    matches
                }
        }
        val allSingletons = (foundSingletons + cachedSingletons).distinctBy { it.kotlinFqName }
        singletonsByModule[declaration.kotlinFqName] = allSingletons.map { it.kotlinFqName.asString() }

        return allSingletons
    }

    fun saveModuleDependency(declaration: IrClass, dependencies: ModuleDependencies) {
//        TODO("Not yet implemented")
    }

    fun saveExtensionDependency(declaration: IrFunction, dependencies: ModuleDependencies) {
//        TODO("Not yet implemented")
    }
}

fun incrementalCache(configuration: CompilerConfiguration): IncrementalCache? {
    val get = configuration.get(DIKT_CACHE)
    return get?.let { IncrementalCache(it) }
}