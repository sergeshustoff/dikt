package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName

object Annotations {
    private val createAnnotation = FqName("dev.shustoff.dikt.Create")
    private val CreateSingleAnnotation = FqName("dev.shustoff.dikt.CreateSingle")
    private val providedAnnotation = FqName("dev.shustoff.dikt.Provide")
    private val useModulesAnnotation = FqName("dev.shustoff.dikt.UseModules")
    val useConstructorsAnnotation = FqName("dev.shustoff.dikt.UseConstructors")
    private val moduleSingletonsAnnotation = FqName("dev.shustoff.dikt.ModuleSingletons")

    fun getUsedModules(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(useModulesAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }

    fun isByDi(descriptor: IrFunction): Boolean {
        return descriptor.annotations.hasAnnotation(createAnnotation)
                || descriptor.annotations.hasAnnotation(CreateSingleAnnotation)
                || descriptor.annotations.hasAnnotation(providedAnnotation)
    }

    fun isCached(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(CreateSingleAnnotation)
    }

    //TODO: rename annotation in a way that implies constructor usage
    fun cachedTypes(module: IrClass): Set<IrType> {
        return module.annotations
            .filter { it.isAnnotation(moduleSingletonsAnnotation) }
            .flatMap { annotation ->
                (annotation.getValueArgument(0) as? IrVararg)
                    ?.elements
                    ?.mapNotNull { (it as? IrClassReference)?.classType }
                    .orEmpty()
            }
            .toSet()
    }

    fun isProvided(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(providedAnnotation)
    }

    fun getProvidedByConstructor(descriptor: IrAnnotationContainer): List<IrType> {
        return descriptor.annotations.filter { it.isAnnotation(useConstructorsAnnotation) }
            .flatMap { annotation ->
                (annotation.getValueArgument(0) as? IrVararg)
                    ?.elements
                    ?.mapNotNull { (it as? IrClassReference)?.classType }
                    .orEmpty()
            }
    }
}