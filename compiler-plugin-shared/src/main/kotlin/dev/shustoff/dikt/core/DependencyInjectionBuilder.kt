package dev.shustoff.dikt.core

import dev.shustoff.dikt.dependency.Dependency
import dev.shustoff.dikt.dependency.ResolvedDependency
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

class DependencyInjectionBuilder(
    private val pluginContext: IrPluginContext
) {
    fun buildPropertyInjections(
        module: IrClass,
        diFunctionSymbols: Set<IrSimpleFunctionSymbol>,
        dependencies: ModuleDependencies
    ) {
        module.properties
            .filter { property -> property.isDelegated && property.backingField != null }
            .forEach { property ->
                val backingField = property.backingField
                val initializerCall = backingField?.initializer?.expression as? IrCall
                if (initializerCall != null && requiresInitializerInjection(initializerCall, diFunctionSymbols)) {
                    buildPropertyInjection(module, property, backingField, initializerCall, dependencies)
                }
            }
    }

    private fun requiresInitializerInjection(
        initializerCall: IrCall,
        diFunctionSymbols: Set<IrSimpleFunctionSymbol>
    ) = initializerCall.symbol in diFunctionSymbols && initializerCall.getValueArgument(0) == null

    private fun buildPropertyInjection(
        declaration: IrClass,
        property: IrProperty,
        backingField: IrField,
        initializerCall: IrCall,
        dependencies: ModuleDependencies
    ) {
        val type = initializerCall.getTypeArgument(0)!!
        val function = backingField.initializer!!.factory.buildFun {
            name = Name.special("<internal_injection_initializer>")
            returnType = type
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply {
            parent = backingField
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val dependency = dependencies.resolveDependency(type, property)
                if (dependency != null) {
                    +irReturn(
                        makeDependencyCall(declaration, dependency)
                    )
                } else {
                    // there should be compilation error anyway in resolveDependency call
                    +irThrow(irNull())
                }
            }
        }
        val functionExpression = IrFunctionExpressionImpl(
            initializerCall.startOffset,
            initializerCall.endOffset,
            IrSimpleTypeImpl(pluginContext.symbols.functionN(0), false, emptyList(), emptyList()),
            function,
            IrStatementOrigin.LAMBDA
        )
        initializerCall.putValueArgument(0, functionExpression)
    }

    private fun IrBlockBodyBuilder.makeDependencyCall(
        module: IrClass,
        dependency: ResolvedDependency
    ): IrExpression = when (dependency.dependency) {
        is Dependency.Constructor -> irCallConstructor(dependency.dependency.constructor.symbol, emptyList()).also {
            for ((index, resolved) in dependency.params.withIndex()) {
                it.putValueArgument(index, makeDependencyCall(module, resolved))
            }
        }
        is Dependency.Function -> irCall(dependency.dependency.function).also {
            if (dependency.dependency.fromNestedModule != null) {
                it.dispatchReceiver = makeGetterCall(dependency.dependency.fromNestedModule, module)
            } else {
                it.dispatchReceiver = IrGetValueImpl(startOffset, endOffset, module.thisReceiver!!.symbol)
            }
            for ((index, resolved) in dependency.params.withIndex()) {
                it.putValueArgument(index, makeDependencyCall(module, resolved))
            }
        }
        is Dependency.Property -> makeGetterCall(dependency.dependency, module)
    }

    private fun IrBlockBodyBuilder.makeGetterCall(
        dependency: Dependency.Property,
        declaration: IrClass
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
        call.dispatchReceiver = IrGetValueImpl(startOffset, endOffset, declaration.thisReceiver!!.symbol)

        return getterCall
    }
}