package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.FqName

object Annotations {
    private val createAnnotation = FqName("dev.shustoff.dikt.Create")
    private val CreateSingleAnnotation = FqName("dev.shustoff.dikt.CreateSingle")
    private val providedAnnotation = FqName("dev.shustoff.dikt.Provide")
    private val useModulesAnnotation = FqName("dev.shustoff.dikt.UseModules")
    private val providesMembersAnnotation = FqName("dev.shustoff.dikt.ProvidesMembers")
    private val injectByConstructorsAnnotation = FqName("dev.shustoff.dikt.InjectByConstructors")
    private val oldUseConstructorsAnnotation = FqName("dev.shustoff.dikt.UseConstructors")
    private val moduleSingletonsAnnotation = FqName("dev.shustoff.dikt.InjectSingleByConstructors")

    fun getUsedModules(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(useModulesAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }

    fun providesMembers(descriptor: IrAnnotationContainer): Boolean {
        return descriptor.hasAnnotation(providesMembersAnnotation)
    }

    fun isByDi(descriptor: IrFunction): Boolean {
        return descriptor.annotations.hasAnnotation(createAnnotation)
                || descriptor.annotations.hasAnnotation(CreateSingleAnnotation)
                || descriptor.annotations.hasAnnotation(providedAnnotation)
    }

    fun isCached(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(CreateSingleAnnotation)
    }

    fun singletonsByConstructor(module: IrClass): Set<FqName> {
        return module.annotations
            .filter { it.isAnnotation(moduleSingletonsAnnotation) }
            .flatMap { annotation ->
                (annotation.getValueArgument(0) as? IrVararg)
                    ?.elements
                    ?.mapNotNull { (it as? IrClassReference)?.classType }
                    .orEmpty()
            }
            .mapNotNull { it.classFqName }
            .toSet()
    }

    fun isProvided(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(providedAnnotation)
    }

    fun getProvidedByConstructor(descriptor: IrAnnotationContainer): List<FqName> {
        return descriptor.annotations.filter { it.isAnnotation(injectByConstructorsAnnotation) || it.isAnnotation(oldUseConstructorsAnnotation) }
            .flatMap { annotation ->
                (annotation.getValueArgument(0) as? IrVararg)
                    ?.elements
                    ?.mapNotNull { (it as? IrClassReference)?.classType }
                    .orEmpty()
            }
            .mapNotNull { it.classFqName }
    }
}