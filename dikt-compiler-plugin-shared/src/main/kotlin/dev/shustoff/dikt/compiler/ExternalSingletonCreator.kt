package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.Annotations
import dev.shustoff.dikt.incremental.IncrementalCache
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.Name

class ExternalSingletonCreator(
    private val errorCollector: ErrorCollector,
    private val pluginContext: IrPluginContext,
    private val singletones: MutableMap<IrType, MutableList<IrClass>>,
    private val incrementalCache: IncrementalCache?
) : IrElementVisitorVoid, ErrorCollector by errorCollector {

    private val singletonAnnotationClass by lazy {
        pluginContext.referenceClass(Annotations.singletonAnnotation)!!.owner
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (Annotations.isModule(declaration)) {
            val foundSingletons = singletones[declaration.defaultType].orEmpty()
            val allSingletons = incrementalCache?.getSingletons(declaration, foundSingletons, pluginContext) ?: foundSingletons
            allSingletons.forEach { singleton ->
                val singletonType = singleton.defaultType
                val functionsOfSameType = declaration.functions.filter { it.returnType == singletonType && it.valueParameters.isEmpty() }.toList()
                if (declaration.properties.any { it.getter?.returnType == singletonType } || functionsOfSameType.any { !Annotations.isSingleton(it) }) {
                    singleton.error("This type is already provided in module ${declaration.defaultType.asString()}")
                } else if (functionsOfSameType.isEmpty()) {
                    declaration.addFunction {
                        name = Name.identifier("provide${singleton.name.asString()}")
                        visibility = DescriptorVisibilities.PUBLIC
                        startOffset = declaration.startOffset
                        endOffset = declaration.endOffset
                        returnType = singletonType
                    }.also {
                        val constructor = singletonAnnotationClass.primaryConstructor!!
                        it.dispatchReceiverParameter = declaration.thisReceiver!!.copyTo(it)
                        it.annotations += IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
                    }
                }
            }
        }
        super.visitClass(declaration)
    }
}