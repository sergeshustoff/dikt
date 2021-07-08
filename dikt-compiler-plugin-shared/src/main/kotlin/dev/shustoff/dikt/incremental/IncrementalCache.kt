package dev.shustoff.dikt.incremental

import org.jetbrains.kotlin.config.CompilerConfiguration
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
}

fun incrementalCache(configuration: CompilerConfiguration): IncrementalCache? {
    val get = configuration.get(DIKT_CACHE)
    return get?.let { IncrementalCache(it) }
}