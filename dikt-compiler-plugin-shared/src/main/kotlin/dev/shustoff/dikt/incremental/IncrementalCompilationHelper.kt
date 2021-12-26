package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.dependency.ProvidedDependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName
import java.util.*

class IncrementalCompilationHelper(
    lookupTracker: LookupTracker
) {
    private val lookupHelper = LookupHelper(lookupTracker)

    fun recordFunctionDependency(from: IrFunction, usedDependency: ResolvedDependency?) {
        val queue = LinkedList(listOfNotNull(usedDependency))
        val recordedLookups = mutableSetOf<FqName>()
        while (queue.isNotEmpty()) {
            when (val resolved = queue.pop()) {
                is ResolvedDependency.Provided -> {
                    when (val provided = resolved.provided) {
                        is ProvidedDependency.Function,
                        is ProvidedDependency.Property -> {
                            provided.irElement.fqNameWhenAvailable?.let {
                                if (it !in recordedLookups) {
                                    recordedLookups.add(it)
                                    lookupHelper.recordLookup(from, it)
                                }
                            }
                        }
                        is ProvidedDependency.Parameter -> {
                            // nop
                        }
                    }
                    queue.addAll(resolved.params)
                    if (resolved.nestedModulesChain != null) {
                        queue.add(resolved.nestedModulesChain)
                    }
                }
                is ResolvedDependency.Constructor -> {
                    resolved.constructor.returnType.classFqName?.let {
                        if (it !in recordedLookups) {
                            recordedLookups.add(it)
                            lookupHelper.recordLookup(from, it)
                        }
                    }
                    queue.addAll(resolved.params)
                }
                is ResolvedDependency.ParameterDefaultValue -> {
                    // nop
                }
            }
        }
    }
}

fun incrementalHelper(
    configuration: CompilerConfiguration
): IncrementalCompilationHelper? {
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return null
    return IncrementalCompilationHelper(lookupTracker)
}