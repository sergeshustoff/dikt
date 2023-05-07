package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.ProvidedDependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import dev.shustoff.dikt.message_collector.ErrorCollector
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class InjectionBuilder(
    private val pluginContext: IrPluginContext,
    errorCollector: ErrorCollector
) : ErrorCollector by errorCollector {

    private val lazyFunction by lazy {
        pluginContext.referenceFunctions(CallableId(FqName("kotlin"), Name.identifier("lazy"))).firstOrNull()?.owner
    }

    fun buildResolvedDependencyCall(
        module: IrClass?,
        function: IrFunction,
        dependency: ResolvedDependency?,
        original: IrCall
    ) : IrExpression {
        function.info("resolving dependency in function ${function.kotlinFqName.asString()}")
        if (dependency == null) {
            return DeclarationIrBuilder(pluginContext, function.symbol).irThrowNoDependencyException(function)
        }
        val context = Context(module, function, original)
        return createDependencyCall(context, dependency)
    }


    private fun createSingletonCall(
        context: Context,
        dependency: ResolvedDependency.Constructor,
        receiverParameter: IrValueParameter?,
    ): IrCall {
        val field = getOrCreateLazyFieldForSingleton(context, dependency)
        val getValueFunction = field.type.getClass()!!.properties.first { it.name.identifier == "value" }.getter!!
        return with(DeclarationIrBuilder(pluginContext, field.symbol)) {
            irCall(getValueFunction.symbol, context.original.type).also {
                it.dispatchReceiver = irGetField(IrGetValueImpl(startOffset, endOffset, (receiverParameter ?: context.module!!.thisReceiver)!!.symbol), field)
            }
        }
    }

    private fun getOrCreateLazyFieldForSingleton(
        context: Context,
        dependency: ResolvedDependency.Constructor,
    ): IrField {
        val module = context.module ?: throw IllegalStateException("This shouldn't happen. Trying to create singleton without containing module")
        val dependencyType = dependency.type
        val fieldIdentifier = Name.identifier("__di_cache__${dependencyType.classFqName!!.asString().replace(".", "_")}")
        val existingField = module.fields.firstOrNull { it.name == fieldIdentifier }
        if (existingField != null) return existingField

        val lazyFunction = lazyFunction
        check(lazyFunction != null) { "kotlin.Lazy not found" }
        val lazyType = lazyFunction.returnType.getClass()!!.typeWith(dependencyType)
        val field = module.addField {
            type = lazyType
            name = fieldIdentifier
            visibility = DescriptorVisibilities.PRIVATE
            startOffset = module.startOffset
            endOffset = module.endOffset
        }

        val factoryFunction = field.factory.buildFun {
            name = Name.special("<internal_injection_initializer>")
            returnType = dependencyType
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply {
            parent = field
            val scopedIrBuilder = DeclarationIrBuilder(pluginContext, symbol)

            body = scopedIrBuilder.irBlockBody {
                +irReturn(
                    scopedIrBuilder.makeConstructorDependencyCall(
                        context = context,
                        constructor = dependency.constructor,
                        params = dependency.params,
                        receiverParameter = dispatchReceiverParameter ?: module.thisReceiver!!,
                    )
                )
            }
        }
        field.initializer = with(DeclarationIrBuilder(pluginContext, field.symbol)) {
            val functionExpression = IrFunctionExpressionImpl(
                startOffset,
                endOffset,
                pluginContext.irBuiltIns.functionN(0).typeWith(dependencyType),
                factoryFunction,
                IrStatementOrigin.LAMBDA
            )
            irExprBody(
                irCall(lazyFunction.symbol, lazyType).also {
                    it.putTypeArgument(0, dependencyType)
                    it.putValueArgument(0, functionExpression)
                }
            )
        }
        return field
    }

    private fun createDependencyCall(
        context: Context,
        dependency: ResolvedDependency,
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, context.forFunction.symbol).makeDependencyCall(
            context = context,
            resolved = dependency,
            receiverParameter = context.forFunction.dispatchReceiverParameter ?: context.module?.thisReceiver,
        )
    }

    private fun DeclarationIrBuilder.irThrowNoDependencyException(function: IrFunction): IrExpression {
        val constructor = context.irBuiltIns.throwableClass.owner.constructors
            .firstOrNull { it.valueParameters.size == 1 && it.valueParameters.first().type.isNullableString() }
        if (!hasErrors()) {
            function.error("No dependency found. Please report bug to DI.kt library, more detailed message is missing for some reason")
        }
        return irThrow(irCallConstructor(constructor!!.symbol, emptyList()).apply {
            putValueArgument(
                0, IrConstImpl.string(
                    startOffset, endOffset, context.irBuiltIns.stringType,
                    "Missing dependency in function ${function.name.asString()}, should be compilation error. Please report bug to DI.kt library"
                )
            )
        })
    }

    private fun DeclarationIrBuilder.makeDependencyCall(
        context: Context,
        resolved: ResolvedDependency,
        receiverParameter: IrValueParameter?,
    ): IrExpression {
        return when (resolved) {
            is ResolvedDependency.Constructor -> if (resolved.isSingleton) {
                createSingletonCall(context, resolved, receiverParameter)
            } else {
                makeConstructorDependencyCall(
                    context,
                    resolved.constructor,
                    resolved.params,
                    receiverParameter,
                )
            }
            is ResolvedDependency.ParameterDefaultValue -> {
                // this should not happen
                context.forFunction.error("Default value wasn't handled correctly, please report bug in DI.kt library")
                resolved.defaultValue.expression
            }
            is ResolvedDependency.Provided -> {
                when (val provided = resolved.provided) {
                    is ProvidedDependency.Function -> makeFunctionDependencyCall(
                        context,
                        provided,
                        receiverParameter,
                        resolved.params,
                        resolved.extensionParam,
                        resolved.nestedModulesChain,
                    )
                    is ProvidedDependency.Property -> makePropertyDependencyCall(
                        context,
                        provided,
                        receiverParameter,
                        resolved.extensionParam,
                        resolved.nestedModulesChain,
                    )
                    is ProvidedDependency.Parameter -> makeParameterCall(provided)
                }
            }
        }
    }

    private fun DeclarationIrBuilder.makeParameterCall(dependency: ProvidedDependency.Parameter): IrExpression {
        return irGet(dependency.parameter)
    }

    private fun DeclarationIrBuilder.makeConstructorDependencyCall(
        context: Context,
        constructor: IrConstructor,
        params: List<ResolvedDependency>,
        receiverParameter: IrValueParameter?,
    ): IrConstructorCall {
        return irCallConstructor(constructor.symbol, emptyList()).also {
            for ((index, resolved) in params.withIndex()) {
                if (resolved !is ResolvedDependency.ParameterDefaultValue) {
                    it.putValueArgument(index, makeDependencyCall(context, resolved, receiverParameter))
                }
            }
        }
    }

    private fun DeclarationIrBuilder.makeFunctionDependencyCall(
        context: Context,
        dependency: ProvidedDependency.Function,
        receiverParameter: IrValueParameter?,
        params: List<ResolvedDependency>,
        extensionParam: ResolvedDependency?,
        nestedModulesChain: ResolvedDependency?,
    ): IrFunctionAccessExpression {
        val call = irCall(dependency.function.symbol, dependency.returnType).also {
            for ((index, resolved) in params.withIndex()) {
                if (resolved !is ResolvedDependency.ParameterDefaultValue) {
                    it.putValueArgument(index, makeDependencyCall(context, resolved, receiverParameter))
                }
            }
        }
        call.extensionReceiver = extensionParam?.let { makeDependencyCall(context, it, receiverParameter) }
        call.dispatchReceiver = nestedModulesChain?.let {
            makeDependencyCall(context, nestedModulesChain, receiverParameter)
        } ?: IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)
        return call
    }

    private fun DeclarationIrBuilder.makePropertyDependencyCall(
        context: Context,
        dependency: ProvidedDependency.Property,
        receiverParameter: IrValueParameter?,
        extensionParam: ResolvedDependency?,
        nestedModulesChain: ResolvedDependency?,
    ): IrFunctionAccessExpression {
        val call = irCall(dependency.property.getter!!.symbol, dependency.returnType)
        val parentCall = nestedModulesChain?.let { makeDependencyCall(context, it, receiverParameter) }
        call.dispatchReceiver = parentCall ?: IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)
        call.extensionReceiver = extensionParam?.let { makeDependencyCall(context, it, receiverParameter) }

        return call
    }

    private data class Context(
        val module: IrClass?,
        val forFunction: IrFunction,
        val original: IrCall,
    )
}