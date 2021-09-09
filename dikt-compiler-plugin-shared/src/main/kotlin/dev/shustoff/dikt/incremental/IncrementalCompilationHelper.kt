package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.kotlinFqName
import java.util.*

class IncrementalCompilationHelper(
    lookupTracker: LookupTracker
) {
    private val lookupHelper = LookupHelper(lookupTracker)

    fun recordFunctionDependency(from: IrFunction, usedDependency: ResolvedDependency?) {
        val dependencies = flattenDependency(listOfNotNull(usedDependency))
        for (dependency in dependencies) {
            when (dependency) {
                is Dependency.Constructor -> lookupHelper.recordLookup(from, dependency.id.type.classFqName!!)
                is Dependency.Function -> {
                    if (dependency.fromNestedModule != null) {
                        lookupHelper.recordLookup(from, dependency.function.kotlinFqName)
                    }
                }
                is Dependency.Property -> {
                    if (dependency.fromNestedModule != null) {
                        lookupHelper.recordLookup(from, dependency.property.fqNameWhenAvailable!!)
                    }
                }
                is Dependency.Parameter -> {
                    // nop
                }
            }
        }
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
}

fun incrementalHelper(
    configuration: CompilerConfiguration
): IncrementalCompilationHelper? {
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return null
    return IncrementalCompilationHelper(lookupTracker)
}