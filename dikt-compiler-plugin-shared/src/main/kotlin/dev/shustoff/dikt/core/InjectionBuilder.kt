package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.ProvidedDependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class InjectionBuilder(
    private val pluginContext: IrPluginContext,
    errorCollector: ErrorCollector
) : ErrorCollector by errorCollector {

    private val lazyFunction by lazy {
        pluginContext.referenceFunctions(FqName("kotlin.lazy")).firstOrNull()?.owner
    }

    fun buildModuleFunctionInjections(
        module: IrClass?,
        function: IrFunction,
        dependency: ResolvedDependency?
    ) {
        function.info("generating function body for ${function.kotlinFqName.asString()}")
        function.body = if (Annotations.isCached(function) && module != null) {
            createSingletonBody(function, dependency, module)
        } else {
            createFactoryBody(function, dependency, module)
        }
    }

    private fun createSingletonBody(
        function: IrFunction,
        dependency: ResolvedDependency?,
        module: IrClass,
    ): IrBlockBody {
        val field = createLazyFieldForSingleton(function, module, dependency)
        val getValueFunction = field.type.getClass()!!.properties.first { it.name.identifier == "value" }.getter!!
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            +irReturn(
                irCall(getValueFunction.symbol, function.returnType).also {
                    it.dispatchReceiver = irGetField(IrGetValueImpl(startOffset, endOffset, (function.dispatchReceiverParameter ?: module.thisReceiver)!!.symbol), field)
                }
            )
        }
    }

    private fun createLazyFieldForSingleton(
        function: IrFunction,
        module: IrClass,
        dependency: ResolvedDependency?,
    ): IrField {
        val lazyFunction = lazyFunction
        check(lazyFunction != null) { "kotlin.Lazy not found" }
        val lazyType = lazyFunction.returnType.getClass()!!.typeWith(function.returnType)
        val field = module.addField {
            type = lazyType
            name = Name.identifier("__di_cache__${function.name.asString()}")
            visibility = DescriptorVisibilities.PRIVATE
            startOffset = function.startOffset
            endOffset = function.endOffset
        }
        field.initializer = with(DeclarationIrBuilder(pluginContext, field.symbol)) {
            val factoryFunction = field.factory.buildFun {
                name = Name.special("<internal_injection_initializer>")
                returnType = function.returnType
                visibility = DescriptorVisibilities.LOCAL
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            }.apply {
                parent = field
                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    if (dependency != null) {
                        +irReturn(
                            makeDependencyCall(dependency, dispatchReceiverParameter ?: module.thisReceiver!!, function)
                        )
                    } else {
                        // there should be compilation error anyway in resolveDependency call
                        +irThrow(irNull()) //TODO:later throw a proper error just in case
                    }
                }
            }
            val functionExpression = IrFunctionExpressionImpl(
                startOffset,
                endOffset,
                pluginContext.irBuiltIns.functionN(0).typeWith(function.returnType),
                factoryFunction,
                IrStatementOrigin.LAMBDA
            )
            irExprBody(
                irCall(lazyFunction.symbol, lazyType).also {
                    it.putTypeArgument(0, function.returnType)
                    it.putValueArgument(0, functionExpression)
                }
            )
        }
        return field
    }

    private fun createFactoryBody(
        function: IrFunction,
        dependency: ResolvedDependency?,
        module: IrClass?
    ) = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
        if (dependency != null) {
            +irReturn(
                makeDependencyCall(dependency, function.dispatchReceiverParameter ?: module?.thisReceiver, function)
            )
        } else {
            // there should be compilation error anyway in resolveDependency call
            +irThrow(irNull()) //TODO:later throw a proper error just in case
        }
    }

    private fun IrBlockBodyBuilder.makeDependencyCall(
        resolved: ResolvedDependency,
        receiverParameter: IrValueParameter?,
        forDiFunction: IrFunction
    ): IrExpression {
        return when (resolved) {
            is ResolvedDependency.Constructor -> makeConstructorDependencyCall(
                resolved.constructor,
                resolved.params,
                receiverParameter,
                forDiFunction
            )
            is ResolvedDependency.ParameterDefaultValue -> {
                // this should not happen
                forDiFunction.error("Default value wasn't handled correctly, please report bug in DI.kt library")
                resolved.defaultValue.expression
            }
            is ResolvedDependency.Provided -> {
                when (val provided = resolved.provided) {
                    is ProvidedDependency.Function -> makeFunctionDependencyCall(
                        provided,
                        receiverParameter,
                        resolved.params,
                        resolved.extensionParam,
                        resolved.nestedModulesChain,
                        forDiFunction
                    )
                    is ProvidedDependency.Property -> makePropertyDependencyCall(
                        provided,
                        receiverParameter,
                        resolved.extensionParam,
                        resolved.nestedModulesChain,
                        forDiFunction
                    )
                    is ProvidedDependency.Parameter -> makeParameterCall(provided)
                }
            }
        }
    }

    private fun IrBlockBodyBuilder.makeParameterCall(dependency: ProvidedDependency.Parameter): IrExpression {
        return irGet(dependency.parameter)
    }

    private fun IrBlockBodyBuilder.makeConstructorDependencyCall(
        constructor: IrConstructor,
        params: List<ResolvedDependency>,
        receiverParameter: IrValueParameter?,
        forDiFunction: IrFunction
    ): IrConstructorCall {
        return irCallConstructor(constructor.symbol, emptyList()).also {
            for ((index, resolved) in params.withIndex()) {
                if (resolved !is ResolvedDependency.ParameterDefaultValue) {
                    it.putValueArgument(index, makeDependencyCall(resolved, receiverParameter, forDiFunction))
                }
            }
        }
    }

    private fun IrBlockBodyBuilder.makeFunctionDependencyCall(
        dependency: ProvidedDependency.Function,
        receiverParameter: IrValueParameter?,
        params: List<ResolvedDependency>,
        extensionParam: ResolvedDependency?,
        nestedModulesChain: ResolvedDependency?,
        forDiFunction: IrFunction
    ): IrFunctionAccessExpression {
        val call = irCall(dependency.function.symbol, dependency.returnType).also {
            for ((index, resolved) in params.withIndex()) {
                if (resolved !is ResolvedDependency.ParameterDefaultValue) {
                    it.putValueArgument(index, makeDependencyCall(resolved, receiverParameter, forDiFunction))
                }
            }
        }
        call.extensionReceiver = extensionParam?.let { makeDependencyCall(it, receiverParameter, forDiFunction) }
        call.dispatchReceiver = nestedModulesChain?.let {
            makeDependencyCall(nestedModulesChain, receiverParameter, forDiFunction)
        } ?: IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)
        return call
    }

    private fun IrBlockBodyBuilder.makePropertyDependencyCall(
        dependency: ProvidedDependency.Property,
        receiverParameter: IrValueParameter?,
        extensionParam: ResolvedDependency?,
        nestedModulesChain: ResolvedDependency?,
        forDiFunction: IrFunction
    ): IrFunctionAccessExpression {
        val call = irCall(dependency.property.getter!!.symbol, dependency.returnType)
        val parentCall = nestedModulesChain?.let { makeDependencyCall(it, receiverParameter, forDiFunction) }
        call.dispatchReceiver = parentCall ?: IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)
        call.extensionReceiver = extensionParam?.let { makeDependencyCall(it, receiverParameter, forDiFunction) }

        return call
    }
}