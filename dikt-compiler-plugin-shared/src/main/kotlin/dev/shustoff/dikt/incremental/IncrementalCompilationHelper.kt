package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.core.ModuleDependencies
import dev.shustoff.dikt.core.VisibilityChecker
import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.*

class IncrementalCompilationHelper(
    cacheDir: File,
    lookupTracker: LookupTracker,
    private val errorCollector: ErrorCollector
) {
    // Something that had or has SingletonIn (added/changed/removed) -> consider target module changed
    // Something created by constructor (changed) -> recompile module that called the constructor (but not module's dependencies)
    // Module (changed) -> recompile all modules that depended on it in case it provides dependency to replace a constructor call

    private val singletonsByModule = ClassToStringsListMap(File(cacheDir, "singletonsByModule"))

    private val modulesDependentOnModule = ClassToStringsListMap(File(cacheDir, "classesDependentOnModule"))
    private val functionsDependentOnModule = ClassToStringsListMap(File(cacheDir, "functionsDependentOnModule"))
    private val modulesDependentOnSingletons = ClassToStringsListMap(File(cacheDir, "classesDependentOnSingletons"))
    private val functionsDependentSingletons = ClassToStringsListMap(File(cacheDir, "functionsDependentSingletons"))
    private val pathByFqName = ClassToPathMap(File(cacheDir, "pathByFqName"))

    private val lookupHelper = LookupHelper(lookupTracker)

    private val changedSingletons = mutableSetOf<FqName>()

    fun flush() {
        singletonsByModule.flush(false)
        modulesDependentOnModule.flush(false)
        functionsDependentOnModule.flush(false)
        modulesDependentOnSingletons.flush(false)
        functionsDependentSingletons.flush(false)
        pathByFqName.flush(false)
    }

    fun updateSingletonsCache(
        module: IrClass,
        foundSingletons: List<IrClass>,
        pluginContext: IrPluginContext,
    ) {
        val oldCache = singletonsByModule[module.kotlinFqName].toSet()
        val cachedSingletons = getCachedSingletons(module, pluginContext)
        val allSingletons = (foundSingletons + cachedSingletons).distinctBy { it.kotlinFqName }
        val newCache = allSingletons.map { it.kotlinFqName.asString() }
        singletonsByModule[module.kotlinFqName] = newCache
        changedSingletons.addAll((oldCache.filter { it !in newCache } + newCache.filter { it !in oldCache }).map { FqName(it) })
    }

    fun getCachedSingletons(
        module: IrClass,
        pluginContext: IrPluginContext
    ) = singletonsByModule[module.kotlinFqName].mapNotNull {
        pluginContext.referenceClass(FqName(it))?.owner
            ?.takeIf {
                val cachedSingletonModule = Annotations.getSingletonModule(it)
                cachedSingletonModule == module.defaultType
            }
    }

    fun recordExtensionDependency(extension: IrFunction, availableDependency: ModuleDependencies, usedDependency: ResolvedDependency?) {
        val used = collectUsedDependency(listOfNotNull(usedDependency))

        val fqName = extension.kotlinFqName
        pathByFqName[fqName] = extension.file.path
        availableDependency.getAllModules().forEach { module ->
            functionsDependentOnModule.add(module.classFqName!!, fqName.asString())
        }
        used.singletons.forEach { singleton ->
            functionsDependentSingletons.add(singleton.classFqName!!, fqName.asString())
        }

        used.constructors.forEach { type ->
            lookupHelper.recordConstructorDependency(extension, type)
        }
    }

    fun recordModuleDependency(module: IrClass, availableDependency: ModuleDependencies, usedDependency: List<ResolvedDependency>) {
        val used = collectUsedDependency(usedDependency)

        val fqName = module.kotlinFqName
        pathByFqName[fqName] = module.file.path
        availableDependency.getAllModules().forEach { module ->
            modulesDependentOnModule.add(module.classFqName!!, fqName.asString())
        }
        used.singletons.forEach { singleton ->
            modulesDependentOnSingletons.add(singleton.classFqName!!, fqName.asString())
        }

        used.constructors.forEach { type ->
            lookupHelper.recordConstructorDependency(module, type)
        }
    }

    private fun collectUsedDependency(
        usedDependency: List<ResolvedDependency>
    ): UsedDependency {
        val constructors = mutableListOf<IrType>()
        val usedSingletons = mutableListOf<IrType>()

        val queue = LinkedList(usedDependency)
        while (queue.isNotEmpty()) {
            val resolved = queue.pop()
            when (val dependency = resolved.dependency) {
                is Dependency.Constructor -> constructors.add(dependency.constructor.returnType)
                is Dependency.Function -> {
                    dependency.function
                        .takeIf { Annotations.isSingleton(it) }
                        ?.returnType?.getClass()
                        ?.takeIf { Annotations.hasSingletonInAnnotation(it) }
                        ?.let {
                            usedSingletons.add(dependency.function.returnType)
                        }
                }
            }
            queue.addAll(resolved.params)
        }

        return UsedDependency(constructors.distinct(), usedSingletons.distinct())
    }

    fun markChangedDependenciesForRecompilation(
        pluginContext: IrPluginContext,
        visitedModules: Set<IrClass>
    ) {
        //TODO: filter only changed modules
        visitedModules.forEach { module ->
            val classes = modulesDependentOnModule[module.kotlinFqName]
                .mapNotNull { pluginContext.referenceClass(FqName(it))?.owner }
                .filter { Annotations.isModule(it) }
            val functions = functionsDependentOnModule[module.kotlinFqName]
                .flatMap { pluginContext.referenceFunctions(FqName(it)) }
                .map { it.owner }
                .filter { Annotations.isProvidedByDi(it) }

            modulesDependentOnModule[module.kotlinFqName] = classes.map { it.kotlinFqName.asString() }.distinct()
            functionsDependentOnModule[module.kotlinFqName] = functions.map { it.kotlinFqName.asString() }.distinct()

            classes.forEach { clazz ->
                pathByFqName[clazz.kotlinFqName]?.let { path ->
                    lookupHelper.recordFullSignatureDependency(clazz, module, VisibilityChecker(clazz), path)
                }
            }
            functions.forEach { func ->
                pathByFqName[func.kotlinFqName]?.let { path ->
                    lookupHelper.recordFullSignatureDependency(func, module, VisibilityChecker(func), path)
                }
            }
        }
        changedSingletons.forEach { singletonFqName ->
            val singletonClass = pluginContext.referenceClass(singletonFqName)?.owner
            if (singletonClass == null) {
                functionsDependentSingletons.remove(singletonFqName)
                modulesDependentOnSingletons.remove(singletonFqName)
                return@forEach
            }

            val classes = modulesDependentOnSingletons[singletonFqName]
                .mapNotNull { pluginContext.referenceClass(FqName(it))?.owner }
                .filter { Annotations.isModule(it) }
            val functions = functionsDependentSingletons[singletonFqName]
                .flatMap { pluginContext.referenceFunctions(FqName(it)) }
                .map { it.owner }
                .filter { Annotations.isProvidedByDi(it) }

            if (Annotations.hasSingletonInAnnotation(singletonClass)) {
                modulesDependentOnSingletons[singletonFqName] = classes.map { it.kotlinFqName.asString() }.distinct()
                functionsDependentSingletons[singletonFqName] = functions.map { it.kotlinFqName.asString() }.distinct()
            } else {
                modulesDependentOnSingletons.remove(singletonFqName)
                functionsDependentSingletons.remove(singletonFqName)
            }

            classes.forEach { clazz ->
                pathByFqName[clazz.kotlinFqName]?.let { path ->
                    errorCollector.info("Recording constructor dependency $path to $singletonFqName")
                    lookupHelper.recordConstructorDependency(clazz, singletonClass, path)
                }
            }
            functions.forEach { func ->
                pathByFqName[func.kotlinFqName]?.let { path ->
                    errorCollector.info("Recording constructor dependency $path to $singletonFqName")
                    lookupHelper.recordConstructorDependency(func, singletonClass, path)
                }
            }
        }
    }

    private class UsedDependency(
        val constructors: List<IrType>,
        val singletons: List<IrType>
    )
}

fun incrementalHelper(configuration: CompilerConfiguration, errorCollector: ErrorCollector): IncrementalCompilationHelper? {
    val cache = configuration.get(DIKT_CACHE) ?: return null
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return null
    return IncrementalCompilationHelper(cache, lookupTracker, errorCollector)
}