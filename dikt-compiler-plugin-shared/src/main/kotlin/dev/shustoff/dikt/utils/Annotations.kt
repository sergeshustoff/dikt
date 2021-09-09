package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

object Annotations {
    private val createAnnotation = FqName("dev.shustoff.dikt.Create")
    private val createCachedAnnotation = FqName("dev.shustoff.dikt.CreateCached")
    private val providedAnnotation = FqName("dev.shustoff.dikt.Provided")
    private val providedCachedAnnotation = FqName("dev.shustoff.dikt.ProvidedCached")
    private val useModulesAnnotation = FqName("dev.shustoff.dikt.UseModules")
    private val useConstructorsAnnotation = FqName("dev.shustoff.dikt.UseConstructors")

    fun getUsedModules(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(useModulesAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }

    fun isByDi(descriptor: IrFunction): Boolean {
        return descriptor.annotations.hasAnnotation(createAnnotation)
                || descriptor.annotations.hasAnnotation(createCachedAnnotation)
                || descriptor.annotations.hasAnnotation(providedAnnotation)
                || descriptor.annotations.hasAnnotation(providedCachedAnnotation)
    }

    fun isCached(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(createCachedAnnotation)
                || descriptor.annotations.hasAnnotation(providedCachedAnnotation)
    }

    fun isProvided(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(providedAnnotation)
                || descriptor.annotations.hasAnnotation(providedCachedAnnotation)
    }

    fun getProvidedByConstructor(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(useConstructorsAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }
}