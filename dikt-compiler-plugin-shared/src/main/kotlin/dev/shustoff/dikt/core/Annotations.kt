package dev.shustoff.dikt.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

object Annotations {
    private val createAnnotation = FqName("dev.shustoff.dikt.Create")
    private val cachedAnnotation = FqName("dev.shustoff.dikt.CreateCached")
    private val providedAnnotation = FqName("dev.shustoff.dikt.Provided")
    private val moduleAnnotation = FqName("dev.shustoff.dikt.DiModule")
    private val providesAllAnnotation = FqName("dev.shustoff.dikt.ProvidesAll")

    fun isModule(declaration: IrClass) = declaration.annotations.hasAnnotation(moduleAnnotation)

    fun doesProvideContent(descriptor: IrDeclarationWithName) =
        descriptor.annotations.hasAnnotation(providesAllAnnotation)

    fun isProvidedByDi(descriptor: CallableMemberDescriptor): Boolean {
        val containingDeclaration = descriptor.containingDeclaration
        return descriptor is FunctionDescriptor
                && (descriptor.annotations.hasAnnotation(createAnnotation)
                || descriptor.annotations.hasAnnotation(cachedAnnotation)
                || descriptor.annotations.hasAnnotation(providedAnnotation))
                && containingDeclaration is ClassDescriptor
                && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
    }

    fun isProvidedByDi(descriptor: IrFunction): Boolean {
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
}