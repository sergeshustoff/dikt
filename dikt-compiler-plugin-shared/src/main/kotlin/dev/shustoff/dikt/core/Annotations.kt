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
    val injectAnnotation = FqName("dev.shustoff.dikt.Inject")
    private val singletonInAnnotation = FqName("dev.shustoff.dikt.SingletonIn")
    private val namedAnnotation = FqName("dev.shustoff.dikt.Named")
    private val injectNamedAnnotation = FqName("dev.shustoff.dikt.InjectNamed")
    private val byDiAnnotation = FqName("dev.shustoff.dikt.ByDi")
    private val moduleAnnotation = FqName("dev.shustoff.dikt.Module")
    val singletonAnnotation = FqName("dev.shustoff.dikt.SingletonByDi")

    fun isModule(declaration: IrClass) = declaration.annotations.hasAnnotation(moduleAnnotation)

    fun isProvidedByDi(descriptor: CallableMemberDescriptor): Boolean {
        val containingDeclaration = descriptor.containingDeclaration
        return descriptor is FunctionDescriptor
                && (descriptor.annotations.hasAnnotation(byDiAnnotation) ||
                (descriptor.annotations.hasAnnotation(singletonAnnotation)
                        && containingDeclaration is ClassDescriptor
                        && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
                        && descriptor.valueParameters.isEmpty()))
    }

    fun isSingleton(descriptor: IrFunction): Boolean {
        return descriptor.annotations.hasAnnotation(singletonAnnotation)
    }

    fun isProvidedByDi(descriptor: IrFunction): Boolean {
        val containingDeclaration = descriptor.parent
        return descriptor.annotations.hasAnnotation(byDiAnnotation) ||
                (descriptor.annotations.hasAnnotation(singletonAnnotation)
                        && containingDeclaration is IrClass
                        && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
                        && descriptor.valueParameters.isEmpty())
    }

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
        return (annotation?.getValueArgument(0) as? IrClassReference)?.classType
    }
}