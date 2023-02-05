package dev.shustoff.dikt.core

import dev.shustoff.dikt.message_collector.ErrorCollector
import dev.shustoff.dikt.utils.Annotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.kClassReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DiOldApiTransformer(
    private val errorCollector: ErrorCollector,
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid(), ErrorCollector by errorCollector {
    private val resolveFunction = pluginContext.referenceFunctions(CallableId(DiNewApiCodeGenerator.diktPackage, Name.identifier(DiNewApiCodeGenerator.diFunctionName))).firstOrNull()
    private val useConstructorAnnotationConstructor= pluginContext.referenceConstructors(ClassId(FqName("dev.shustoff.dikt"), Name.identifier("UseConstructors")))
        .firstOrNull()
    private val moduleSingletonsAnnotationConstructor= pluginContext.referenceConstructors(ClassId(FqName("dev.shustoff.dikt"), Name.identifier("ModuleSingletons")))
        .firstOrNull()
    private val klassType = pluginContext.referenceClass(ClassId(FqName("kotlin.reflect"), Name.identifier("KClass")))

    override fun visitFunction(declaration: IrFunction): IrStatement {
        //TODO: add errors for null stuff
        val declarationIrBuilder = DeclarationIrBuilder(pluginContext, declaration.symbol)
        if (Annotations.isByDi(declaration)) {
            if ((declaration as? IrSimpleFunction)?.modality != Modality.FINAL) {
                declaration.error("Only final functions can have generated body")
            }
            declaration.body = declarationIrBuilder.irBlockBody {
                +irReturn(
                    irCall(resolveFunction!!, declaration.returnType, listOf(declaration.returnType))
                )
            }
            if (!Annotations.isProvided(declaration)) {
                declaration.annotations = declaration.annotations + listOf(
                    declarationIrBuilder.irCallConstructor(useConstructorAnnotationConstructor!!, emptyList())
                        .apply {
                            val vararg = declarationIrBuilder.irVararg(klassType!!.defaultType, listOf(
                                declarationIrBuilder.kClassReference(declaration.returnType)
                            ))
                            putValueArgument(0, vararg)
                        }
                )
            }
            if (Annotations.isCached(declaration)) {
                if (declaration.valueParameters.isNotEmpty()) {
                    declaration.error("@CreateSingle functions can't have parameters")
                }
                val clazz = declaration.parentClassOrNull
                if (clazz == null) {
                    declaration.error("@CreateSingle are only allowed in class functions")
                } else {
                    clazz.annotations = clazz.annotations + listOf(
                        declarationIrBuilder.irCallConstructor(moduleSingletonsAnnotationConstructor!!, emptyList())
                            .apply {
                                val vararg = declarationIrBuilder.irVararg(klassType!!.defaultType, listOf(
                                    declarationIrBuilder.kClassReference(declaration.returnType)
                                ))
                                putValueArgument(0, vararg)
                            }
                    )
                }
            }
        }
        return super.visitFunction(declaration)
    }
}