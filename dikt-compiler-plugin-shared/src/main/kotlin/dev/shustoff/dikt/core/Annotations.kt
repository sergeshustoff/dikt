package dev.shustoff.dikt.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.SimpleType

object Annotations {
    val injectAnnotation = FqName("dev.shustoff.dikt.Inject")
    private val namedAnnotation = FqName("dev.shustoff.dikt.Named")
    private val injectNamedAnnotation = FqName("dev.shustoff.dikt.InjectNamed")
    private val byDiAnnotation = FqName("dev.shustoff.dikt.ByDi")
    private val moduleAnnotation = FqName("dev.shustoff.dikt.Module")
    private val cachedAnnotation = FqName("dev.shustoff.dikt.Cached")

    fun isModule(declaration: IrClass) = declaration.annotations.hasAnnotation(moduleAnnotation)

    fun isProvidedByDi(descriptor: CallableMemberDescriptor): Boolean {
        val containingDeclaration = descriptor.containingDeclaration
        return descriptor is FunctionDescriptor
                && descriptor.annotations.hasAnnotation(byDiAnnotation)
                && containingDeclaration is ClassDescriptor
                && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
    }

    fun isSingleton(descriptor: IrSimpleFunction): Boolean {
        return descriptor.annotations.hasAnnotation(cachedAnnotation)
    }

    fun isProvidedByDi(descriptor: IrSimpleFunction): Boolean {
        val containingDeclaration = descriptor.parent
        return descriptor.annotations.hasAnnotation(byDiAnnotation)
                && containingDeclaration is IrClass
                && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
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
}