package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

object Annotations {
    private val createAnnotation = FqName("dev.shustoff.dikt.Create")
    private val cachedAnnotation = FqName("dev.shustoff.dikt.CreateCached")
    private val providedAnnotation = FqName("dev.shustoff.dikt.Provided")
    private val moduleAnnotation = FqName("dev.shustoff.dikt.DiModule")
    private val withModulesAnnotation = FqName("dev.shustoff.dikt.UseModules")
    private val provideByConstructorAnnotation = FqName("dev.shustoff.dikt.UseConstructors")

    fun isModule(declaration: IrClass) = declaration.annotations.hasAnnotation(moduleAnnotation)

    fun getUsedModules(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(withModulesAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }

    fun isByDi(descriptor: IrFunction): Boolean {
        val containingDeclaration = descriptor.parent
        return (descriptor.annotations.hasAnnotation(createAnnotation)
                || descriptor.annotations.hasAnnotation(cachedAnnotation)
                || descriptor.annotations.hasAnnotation(providedAnnotation))
                && containingDeclaration is IrClass
                && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
    }

    fun isCached(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(cachedAnnotation)
    }

    fun isProviderForExternalDependency(descriptor: IrFunction): Boolean {
        return descriptor.hasAnnotation(providedAnnotation)
    }

    fun getProvidedByConstructor(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(provideByConstructorAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }
}