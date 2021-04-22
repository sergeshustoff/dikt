package dev.shustoff.dikt.core

import dev.shustoff.dikt.compiler.FullCodeDependencyCollector
import dev.shustoff.dikt.compiler.psiElementSafe
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import java.util.*

class RecursiveCallsDetector(
    private val errorCollector: ErrorCollector
) : ErrorCollector by errorCollector {

    fun checkForRecursiveCalls(module: IrClass) {
        val dependencyMap = collectDependencyMap(module)

        findRecursiveCalls(dependencyMap, module)
    }

    private fun findRecursiveCalls(
        dependencyMap: Map<IrDeclarationWithName, Set<IrDeclarationWithName>>,
        module: IrClass
    ) {
        val nodes = dependencyMap
            .filterValues { it.isNotEmpty() }

        val inDegreeCount = nodes.mapValues { 0 }.toMutableMap()
        for (node in nodes.values) {
            for (param in node) {
                if (inDegreeCount.containsKey(param)) {
                    inDegreeCount[param] = inDegreeCount[param]!! + 1
                }
            }
        }
        val queue = LinkedList(inDegreeCount.filterValues { it == 0 }.keys)
        while (queue.isNotEmpty()) {
            val from = queue.pop()
            nodes[from]?.forEach { to ->
                if (inDegreeCount.containsKey(to)) {
                    inDegreeCount[to] = inDegreeCount[to]!! - 1
                    if (inDegreeCount[to] == 0) {
                        queue.add(to)
                    }
                }
            }
        }
        val declarationsWithCycles = inDegreeCount.filterValues { it > 0 }.keys
        for (declaration in declarationsWithCycles) {
            declaration.psiElementSafe.error(
                "Recursive dependency in ${declaration.name} in module ${module.name.asString()}",
            )
        }
    }

    private fun collectDependencyMap(module: IrClass): Map<IrDeclarationWithName, Set<IrDeclarationWithName>> {
        val properties = module.properties
        val functions = module.functions.filter { !it.isFakeOverride }
        val symbols: Map<IrSymbol, IrDeclarationWithName> =
            properties.associateBy { it.getter!!.symbol } +
                    functions.associateBy { it.symbol }
        val visitor = FullCodeDependencyCollector(symbols)
        return (properties + functions)
            .map { declaration ->
                val collected = mutableSetOf<IrDeclarationWithName>()
                declaration.acceptChildren(visitor, collected)
                declaration to collected
            }.toMap()
    }
}