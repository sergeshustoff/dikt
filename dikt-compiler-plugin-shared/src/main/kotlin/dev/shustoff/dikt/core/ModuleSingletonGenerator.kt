package dev.shustoff.dikt.core

import dev.shustoff.dikt.incremental.IncrementalCompilationHelper
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

class ModuleSingletonGenerator(
    private val pluginContext: IrPluginContext,
    errorCollector: ErrorCollector,
    private val incrementalCompilationHelper: IncrementalCompilationHelper?
) : ErrorCollector by errorCollector {

    private val singletonAnnotationClass by lazy {
        pluginContext.referenceClass(Annotations.singletonAnnotation)!!.owner
    }

    private val handledModules = mutableSetOf<IrClass>()

    fun generateModuleSingletonsIfNotGeneratedYet(
        module: IrClass,
    ) {
        if (module !in handledModules && incrementalCompilationHelper != null) {
            val singletons = incrementalCompilationHelper.getCachedSingletons(module, pluginContext)
            generateModuleSingletons(module, singletons)
        }
    }

    fun generateModuleSingletons(
        module: IrClass,
        singletons: List<IrClass>
    ) {
        handledModules.add(module)
        singletons.forEach { singleton ->
            val singletonType = singleton.defaultType
            val functionsOfSameType =
                module.functions.filter { it.returnType == singletonType && it.valueParameters.isEmpty() }.toList()
            if (module.properties.any { it.getter?.returnType == singletonType } || functionsOfSameType.any {
                    !Annotations.isSingleton(
                        it
                    )
                }) {
                singleton.error("This type is already provided in module ${module.defaultType.asString()}")
            } else if (functionsOfSameType.isEmpty()) {
                module.addFunction {
                    name = singletonFunName(singleton)
                    visibility = DescriptorVisibilities.PUBLIC
                    startOffset = module.startOffset
                    endOffset = module.endOffset
                    returnType = singletonType
                }.also {
                    val constructor = singletonAnnotationClass.primaryConstructor!!
                    it.dispatchReceiverParameter = module.thisReceiver!!.copyTo(it)
                    it.annotations += IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
                }
            }
        }
    }

    companion object {
        fun singletonFunName(singleton: IrClass) =
            Name.identifier("provide${singleton.name.asString()}")
    }
}