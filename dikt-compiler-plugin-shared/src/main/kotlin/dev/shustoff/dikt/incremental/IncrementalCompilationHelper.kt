package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.synthetic.isVisibleOutside
import java.io.File
import java.util.*

class IncrementalCompilationHelper(
    cacheDir: File,
    lookupTracker: LookupTracker,
    private val errorCollector: ErrorCollector
) {
    // if any type used in ByDi changed constructor signature or annotations -> recompile ByDi
    // if any module has added/changed/removed fun/property providing certain type: rebuild all ByDi that uses type regardless of access to module
    // TODO:later recompile only if has access to changed module?

    private val singletonsByModule = ClassToStringsListMap(File(cacheDir, "singletonsByModule"))
    private val classifiersByModule = ClassToStringsListMap(File(cacheDir, "classifiersByModule"))
    private val returnedTypesByModule = ClassToStringsListMap(File(cacheDir, "returnedTypesByModule"))

    //TODO:later clean it somehow to remove deleted dependency?
    private val dependencyByUsedType = ClassToStringsListMap(File(cacheDir, "dependencyByUsedType"))

    private val pathByFqName = ClassToPathMap(File(cacheDir, "pathByFqName"))

    private val lookupHelper = LookupHelper(lookupTracker)

    fun flush() {
        singletonsByModule.flush(false)
        classifiersByModule.flush(false)
        returnedTypesByModule.flush(false)
        dependencyByUsedType.flush(false)
        pathByFqName.flush(false)
    }

    fun updateModuleCache(
        modules: Collection<IrClass>,
        foundSingletons: Map<IrType, List<IrClass>>,
        pluginContext: IrPluginContext,
    ) {
        classifiersByModule.keys().forEach { fqName ->
            val module = pluginContext.referenceClass(fqName)?.owner
            if (module == null || !Annotations.isModule(module)) {
                returnedTypesByModule[fqName].forEach { oldReturnedType ->
                    dependencyByUsedType[FqName(oldReturnedType)].forEach { dependency ->
                        pathByFqName[FqName(dependency)]?.let { path ->
                            lookupHelper.recordLookup(path, fqName)
                        }
                    }
                }
                classifiersByModule.remove(fqName)
                singletonsByModule.remove(fqName)
                returnedTypesByModule.remove(fqName)
            }
        }
        modules.forEach { module ->
            val fqName = module.kotlinFqName
            val oldModuleClassifiers = classifiersByModule[fqName].toSet()
            val newClassifiers = getModuleClassifiers(module)
            val functions = module.functions.filter { it.visibility.isVisibleOutside() }
            val properties = module.properties.filter { it.visibility.isVisibleOutside() }

            pathByFqName[fqName] = module.file.path
            classifiersByModule[fqName] = newClassifiers
            returnedTypesByModule[fqName] =
                (functions.map { it.returnType } + properties.mapNotNull { it.getter?.returnType })
                    .distinct()
                    .mapNotNull { type -> type.classFqName?.asString() }
                    .toSet()

            (functions.filter { it.name.asString() !in oldModuleClassifiers }
                .map { function -> function.returnType.classFqName!! to function.kotlinFqName } +
                    properties.filter { it.name.asString() !in oldModuleClassifiers }
                        .mapNotNull { property -> property.getter?.let { it.returnType.classFqName!! to property.fqNameWhenAvailable!! } })
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .forEach { (typeFq, classifiersFq) ->
                    dependencyByUsedType[typeFq].forEach { dependency ->
                        pathByFqName[FqName(dependency)]?.let { path ->
                            classifiersFq.forEach { classifier ->
                                lookupHelper.recordLookup(path, classifier)
                            }
                        }
                    }
                }
        }

        foundSingletons.forEach { (moduleType, foundModuleSingletons) ->
            val module = moduleType.getClass()!!
            val fqName = moduleType.classFqName!!
            val oldSingletons = singletonsByModule[fqName].toSet()
            pathByFqName[module.kotlinFqName]?.let { path ->
                foundModuleSingletons.forEach { singleton ->
                    if (singleton.kotlinFqName.asString() !in oldSingletons) {
                        // recompile modules with new singletons and add lookup in case singleton changes
                        lookupHelper.recordLookup(path, singleton.kotlinFqName)
                        //TODO: this clearly doesn't work
                    }
                }
            }

            val cachedSingletons = getValidCachedSingletons(module, pluginContext)
            val allSingletons = (foundModuleSingletons + cachedSingletons).distinctBy { it.kotlinFqName }
            val newCache = allSingletons.map { it.kotlinFqName.asString() }
            singletonsByModule[fqName] = newCache
        }
    }

    private fun getModuleClassifiers(module: IrClass): Set<String> {
        return (module.properties.filter { it.visibility.isVisibleOutside() } +
                module.functions.filter { it.visibility.isVisibleOutside() }).map { it.name.asString() }.toSet()
    }

    fun getValidCachedSingletons(
        module: IrClass,
        pluginContext: IrPluginContext
    ) = singletonsByModule[module.kotlinFqName].mapNotNull {
        pluginContext.referenceClass(FqName(it))?.owner
            ?.takeIf {
                val cachedSingletonModule = Annotations.getSingletonModule(it)
                cachedSingletonModule == module.defaultType
            }
    }

    fun recordExtensionDependency(extension: IrFunction, usedDependency: ResolvedDependency?) {
        val resolvedDependencies = registerDependencyLookups(extension, listOfNotNull(usedDependency))
        val dependsOnTypes = resolvedDependencies.mapNotNull { it.id.type.classFqName }.toSet()
        dependsOnTypes.forEach { type ->
            dependencyByUsedType.add(type, extension.kotlinFqName.asString())
            pathByFqName[extension.kotlinFqName] = extension.file.path
        }
    }

    fun recordModuleDependency(module: IrClass, usedDependency: List<ResolvedDependency>) {
        val resolvedDependencies = registerDependencyLookups(module, usedDependency)
        val dependsOnTypes = resolvedDependencies.mapNotNull { it.id.type.classFqName }.toSet()
        dependsOnTypes.forEach { type ->
            dependencyByUsedType.add(type, module.kotlinFqName.asString())
            pathByFqName[module.kotlinFqName] = module.file.path
        }
    }

    private fun registerDependencyLookups(
        from: IrDeclaration,
        usedDependency: List<ResolvedDependency>
    ): Set<Dependency> {
        val dependencies = flattenDependency(usedDependency)
        for (dependency in dependencies) {
            when (dependency) {
                is Dependency.Constructor -> lookupHelper.recordLookup(from.file.path, dependency.id.type.classFqName!!)
                is Dependency.Function -> lookupHelper.recordLookup(from.file.path, dependency.function.kotlinFqName)
                is Dependency.Property -> lookupHelper.recordLookup(
                    from.file.path,
                    dependency.property.fqNameWhenAvailable!!
                )
            }
        }
        return dependencies
    }

    private fun flattenDependency(dependencies: List<ResolvedDependency>): Set<Dependency> {
        val result = mutableSetOf<Dependency>()
        val queue = LinkedList(dependencies)
        while (queue.isNotEmpty()) {
            val resolved = queue.pop()
            val dependency = resolved.dependency
            if (dependency !is Dependency.Property && dependency !in result) {
                result.add(dependency)
                queue.addAll(resolved.params)

                var nestedModule = resolved.nestedModulesChain
                while (nestedModule != null && nestedModule.dependency !in result) {
                    result.add(nestedModule.dependency)
                    queue.addAll(nestedModule.params)
                    nestedModule = nestedModule.nestedModulesChain
                }
            }
        }
        return result
    }

    fun getAvailableModulesList(): List<FqName> {
        return classifiersByModule.keys()
    }

    private fun isBeingRecompiled(declaration: IrDeclaration): Boolean =
        declaration.isInCurrentModule() // may not be the best way, but should work
}

fun incrementalHelper(
    configuration: CompilerConfiguration,
    errorCollector: ErrorCollector
): IncrementalCompilationHelper? {
    val cache = configuration.get(DIKT_CACHE) ?: return null
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return null
    return IncrementalCompilationHelper(cache, lookupTracker, errorCollector)
}