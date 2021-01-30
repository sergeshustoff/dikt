package dev.shustoff.dikt.core

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.SimpleType

object Annotations {
    val injectAnnotation = FqName("dev.shustoff.dikt.Inject")
    private val singletonInAnnotation = FqName("dev.shustoff.dikt.SingletonIn")
    private val namedAnnotation = FqName("dev.shustoff.dikt.Named")
    private val injectNamedAnnotation = FqName("dev.shustoff.dikt.InjectNamed")

    fun getAnnotatedName(element: IrAnnotationContainer): String? {
        val annotation = when (element) {
            is IrValueParameter -> element.getAnnotation(injectNamedAnnotation)
            is IrProperty -> element.getAnnotation(namedAnnotation)
            is IrFunction -> element.getAnnotation(namedAnnotation)
            else -> null
        }

        return (annotation?.getValueArgument(0) as? IrConst<String>)?.value
    }

    fun getSingletonModule(element: IrClass): IrType? {
        val annotation = element.getAnnotation(singletonInAnnotation)
        return (annotation?.type as? IrSimpleType)?.arguments?.get(0)?.typeOrNull
    }
}