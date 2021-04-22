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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DependencyInjectionBuilder(
    private val pluginContext: IrPluginContext,
    errorCollector: ErrorCollector
) : ErrorCollector by errorCollector {

    private val lazyFunction by lazy {
        pluginContext.referenceFunctions(FqName("kotlin.lazy")).firstOrNull()?.owner
    }

    fun buildInjections(
        module: IrClass,
        dependencies: ModuleDependencies
    ) {
        val functions = module.functions.filter { function -> Annotations.isProvidedByDi(function) }.toList()
        functions.forEach { function ->
            function.body = if (Annotations.isSingleton(function)) {
                createSingletonBody(function, dependencies, module)
            } else {
                createFactoryBody(function, dependencies, module)
            }
        }
    }

    private fun createSingletonBody(
        function: IrSimpleFunction,
        dependencies: ModuleDependencies,
        module: IrClass
    ): IrBlockBody {
        //TODO: clean up the mess?
        val lazyFunction = lazyFunction
        check(lazyFunction != null) { "kotlin.Lazy not found" }
        val lazyType = pluginContext.referenceClass(FqName("kotlin.Lazy"))!!.typeWith(function.returnType)
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
                    val dependency = dependencies.resolveDependency(function.returnType, Dependency.Function(function, null))
                    if (dependency != null) {
                        +irReturn(
                            makeDependencyCall(module, dependency, dispatchReceiverParameter)
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
        val getValueFunction = lazyType.getClass()!!.properties.first { it.name.identifier == "value" }.getter!!
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            +irReturn(
                irCall(getValueFunction).also {
                    it.dispatchReceiver = irGetField(
                        IrGetValueImpl(startOffset, endOffset, function.dispatchReceiverParameter!!.symbol),
                        field
                    )
                }
            )
        }
    }

    private fun createFactoryBody(
        function: IrSimpleFunction,
        dependencies: ModuleDependencies,
        module: IrClass
    ) = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
        val dependency =
            dependencies.resolveDependency(function.returnType, Dependency.Function(function, null))
        if (dependency != null) {
            +irReturn(
                makeDependencyCall(module, dependency, function.dispatchReceiverParameter)
            )
        } else {
            // there should be compilation error anyway in resolveDependency call
            +irThrow(irNull())
        }
    }

    private fun IrBlockBodyBuilder.makeDependencyCall(
        module: IrClass,
        dependency: ResolvedDependency,
        receiverParameter: IrValueParameter?
    ): IrExpression {
        return when (dependency.dependency) {
            is Dependency.Constructor -> makeConstructorDependencyCall(dependency.dependency, dependency.params, module, receiverParameter)
            is Dependency.Function -> makeFunctionDependencyCall(dependency.dependency, receiverParameter, dependency.params, module)
            is Dependency.Property -> makePropertyDependencyCall(dependency.dependency, receiverParameter)
        }
    }

    private fun IrBlockBodyBuilder.makeConstructorDependencyCall(
        dependency1: Dependency.Constructor,
        params: List<ResolvedDependency>,
        module: IrClass,
        receiverParameter: IrValueParameter?
    ): IrConstructorCall {
        return irCallConstructor(dependency1.constructor.symbol, emptyList()).also {
            for ((index, resolved) in params.withIndex()) {
                it.putValueArgument(index, makeDependencyCall(module, resolved, receiverParameter))
            }
        }
    }

    private fun IrBlockBodyBuilder.makeFunctionDependencyCall(
        dependency: Dependency.Function,
        receiverParameter: IrValueParameter?,
        params: List<ResolvedDependency>,
        module: IrClass
    ): IrFunctionAccessExpression {
        return irCall(dependency.function).also {
            if (dependency.fromNestedModule != null) {
                it.dispatchReceiver = makePropertyDependencyCall(dependency.fromNestedModule, receiverParameter)
            } else {
                it.dispatchReceiver = IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)
            }
            for ((index, resolved) in params.withIndex()) {
                it.putValueArgument(index, makeDependencyCall(module, resolved, receiverParameter))
            }
        }
    }

    private fun IrBlockBodyBuilder.makePropertyDependencyCall(
        dependency: Dependency.Property,
        receiverParameter: IrValueParameter?
    ): IrFunctionAccessExpression {
        val getterCall = irCall(dependency.property.getter!!)
        var call = getterCall
        var parent = dependency.fromNestedModule
        // recursive call nested modules if needed
        while (parent != null) {
            call = irCall(parent.property.getter!!).also {
                call.dispatchReceiver = it
            }
            parent = parent.fromNestedModule
        }
        call.dispatchReceiver = IrGetValueImpl(startOffset, endOffset, receiverParameter!!.symbol)

        return getterCall
    }
}