package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.ModuleDependencies
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import java.io.File

class IncrementalHelper(
    cacheDir: File,
    private val lookupTracker: LookupTracker,
    private val errorCollector: ErrorCollector
) {
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

    fun recordModuleDependency(declaration: IrDeclaration, dependencies: ModuleDependencies) {
        dependencies.getAllModules()
            .also {
                errorCollector.info("Recording ${it.size} modules for ${declaration.file.path}")
            }
            .forEach { recordTypeLookup(declaration, it) } //TODO: doesn't track all fields and functions
    }

    fun recordTypeLookup(from: IrDeclaration, type: IrType) {
        type.getClass()?.symbol
        val clazz = type.classOrNull?.owner ?: return
        clazz.packageFqName?.let { packageFqName ->
            val position = if (lookupTracker.requiresPosition) {
                Position(from.fileEntry.getLineNumber(from.startOffset),
                    from.fileEntry.getColumnNumber(from.startOffset))
            } else {
                Position.NO_POSITION
            }
            //TODO: check incremental compilation when depend on class without package or nested class
            lookupTracker.record(
                from.file.path,
                position,
                packageFqName.asString(),
                ScopeKind.PACKAGE,
                clazz.name.asString()
            )
        }
    }
}

fun incrementalHelper(configuration: CompilerConfiguration, errorCollector: ErrorCollector): IncrementalHelper? {
    val cache = configuration.get(DIKT_CACHE) ?: return null
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return null
    return IncrementalHelper(cache, lookupTracker, errorCollector)
}