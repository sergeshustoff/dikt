package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
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
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
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

    fun buildInjections(
        module: IrClass,
        dependencies: ModuleDependencies,
    ) {
        val functions = module.functions.filter { function -> Annotations.isProvidedByDi(function) }.toList()
        functions.forEach { function ->
            function.info("generating function body for ${function.kotlinFqName.asString()}")
            function.body = if (Annotations.isSingleton(function)) {
                createSingletonBody(function, dependencies, module)
            } else {
                createFactoryBody(function, dependencies, module)
            }
        }
    }

    fun buildExtensionInjection(
        function: IrFunction,
        dependencies: ModuleDependencies
    ) {
        function.info("generating function body for ${function.kotlinFqName.asString()}")
        function.body = createFactoryBody(function, dependencies, null)
    }

    private fun createSingletonBody(
        function: IrSimpleFunction,
        dependencies: ModuleDependencies,
        module: IrClass,
    ): IrBlockBody {
        val field = createLazyFieldForSingleton(function, module, dependencies)
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
        function: IrSimpleFunction,
        module: IrClass,
        dependencies: ModuleDependencies,
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
                    val dependency =
                        dependencies.resolveDependency(function.returnType, function)
                    if (dependency != null) {
                        +irReturn(
                            makeDependencyCall(dependency, dispatchReceiverParameter ?: module.thisReceiver!!)
                        )
                    } else {
                        // there should be compilation error anyway in resolveDependency call
                        +irThrow(irNull())
                    }
                }
            }
            val functionExpression = IrFunctionExpressionImpl(
                startOffset,
                endOffset,
                IrSimpleTypeImpl(pluginContext.irBuiltIns.function(0), false, emptyList(), emptyList()),
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
        dependencies: ModuleDependencies,
        module: IrClass?
    ) = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
        val dependency =
            dependencies.resolveDependency(function.returnType, function)
        if (dependency != null) {
            +irReturn(
                makeDependencyCall(dependency, function.dispatchReceiverParameter ?: module?.thisReceiver)
            )
        } else {
            // there should be compilation error anyway in resolveDependency call
            +irThrow(irNull())
        }
    }

    private fun IrBlockBodyBuilder.makeDependencyCall(
        dependency: ResolvedDependency,
        receiverParameter: IrValueParameter?
    ): IrExpression {
        return when (dependency.dependency) {
            is Dependency.Constructor -> makeConstructorDependencyCall(
                dependency.dependency,
                dependency.params,
                receiverParameter
            )
            is Dependency.Function -> makeFunctionDependencyCall(
                dependency.dependency,
                receiverParameter,
                dependency.params,
                dependency.nestedModulesChain
            )
            is Dependency.Property -> makePropertyDependencyCall(dependency.dependency, receiverParameter, dependency.nestedModulesChain)
            is Dependency.Parameter -> makeParameterCall(dependency.dependency)
        }
    }

    private fun IrBlockBodyBuilder.makeParameterCall(dependency: Dependency.Parameter): IrExpression {
        return irGet(dependency.parameter)
    }

    private fun IrBlockBodyBuilder.makeConstructorDependencyCall(
        dependency: Dependency.Constructor,
        params: List<ResolvedDependency>,
        receiverParameter: IrValueParameter?
    ): IrConstructorCall {
        return irCallConstructor(dependency.constructor.symbol, emptyList()).also {
            for ((index, resolved) in params.withIndex()) {
                it.putValueArgument(index, makeDependencyCall(resolved, receiverParameter))
            }
        }
    }

    private fun IrBlockBodyBuilder.makeFunctionDependencyCall(
        dependency: Dependency.Function,
        receiverParameter: IrValueParameter?,
        params: List<ResolvedDependency>,
        nestedModulesChain: ResolvedDependency?
    ): IrFunctionAccessExpression {
        val call = irCall(dependency.function.symbol, dependency.returnType).also {
            for ((index, resolved) in params.withIndex()) {
                it.putValueArgument(index, makeDependencyCall(resolved, receiverParameter))
            }
        }
        call.dispatchReceiver = nestedModulesChain?.let { makeDependencyCall(nestedModulesChain, receiverParameter) }
            ?: IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)
        return call
    }

    private fun IrBlockBodyBuilder.makePropertyDependencyCall(
        dependency: Dependency.Property,
        receiverParameter: IrValueParameter?,
        nestedModulesChain: ResolvedDependency?
    ): IrFunctionAccessExpression {
        val call = irCall(dependency.property.getter!!.symbol, dependency.returnType)
        val parentCall = nestedModulesChain?.let { makeDependencyCall(it, receiverParameter) }
        call.dispatchReceiver = parentCall ?: IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)

        return call
    }
}