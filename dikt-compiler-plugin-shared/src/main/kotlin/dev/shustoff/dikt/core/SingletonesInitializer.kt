package dev.shustoff.dikt.core

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.buildTypeProjection
import org.jetbrains.kotlin.ir.types.impl.toBuilder
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

class SingletonesInitializer(
    private val pluginContext: IrPluginContext
) {
    fun initSingletones(module: IrClass, singletones: MutableList<IrClass>?, singletonFunction: IrSimpleFunction) {
        for ((index, singleton) in singletones.orEmpty().withIndex()) {
            val lazyType = (singletonFunction.returnType as IrSimpleType).buildSimpleType {
                arguments = listOf(singleton.defaultType.toBuilder().buildTypeProjection())
            }
            module.addProperty {
                name = Name.identifier("__internal_singleton_$index")
                visibility = DescriptorVisibilities.PUBLIC
                isDelegated = true
                startOffset = module.startOffset
                endOffset = module.endOffset
            }.also { property ->
                val field = property.factory.buildField {
                    name = Name.identifier("${property.name.identifier}\$delegate")
                    visibility = DescriptorVisibilities.PRIVATE
                    type = lazyType
                    origin = IrDeclarationOrigin.PROPERTY_DELEGATE
                    startOffset = module.startOffset
                    endOffset = module.endOffset
                }.apply {
                    parent = module
                    initializer = with(DeclarationIrBuilder(pluginContext, symbol)) {
                        irExprBody(irCall(singletonFunction.symbol, lazyType).also {
                            it.dispatchReceiver = IrGetValueImpl(startOffset, endOffset, module.thisReceiver!!.symbol)
                            it.putTypeArgument(0, singleton.defaultType)
                            it.putValueArgument(0, null)
                        })
                    }
                }
                property.backingField = field
                field.correspondingPropertySymbol = property.symbol
                property.addDelegateFieldGetter(singleton.defaultType, module, field)
            }
        }
    }

    private fun IrProperty.addDelegateFieldGetter(
        type: IrType,
        module: IrClass,
        field: IrField
    ): IrSimpleFunction {
        val getValueFunction = field.type.getClass()!!.properties.first { it.name.identifier == "value" }.getter!!
        return addGetter {
            returnType = type
            origin = IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
            startOffset = module.startOffset
            endOffset = module.endOffset
        }.apply {
            dispatchReceiverParameter = module.thisReceiver!!.copyTo(this)
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irReturn(irCall(getValueFunction.symbol, type).also { call ->
                    call.dispatchReceiver = irGetField(IrGetValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        dispatchReceiverParameter!!.type,
                        dispatchReceiverParameter!!.symbol
                    ), field)
                })
            }
        }
    }
}