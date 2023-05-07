package dev.shustoff.dikt.utils

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

object Annotations {
    private val useModulesAnnotation = FqName("dev.shustoff.dikt.UseModules")
    private val providesMembersAnnotation = FqName("dev.shustoff.dikt.ProvidesMembers")
    private val injectByConstructorsAnnotation = FqName("dev.shustoff.dikt.InjectByConstructors")
    private val moduleSingletonsAnnotation = FqName("dev.shustoff.dikt.InjectSingleByConstructors")
    private val injectable = FqName("dev.shustoff.dikt.Injectable")
    private val injectableSingle = FqName("dev.shustoff.dikt.InjectableSingleInScope")
    private val moduleScopesAnnotation = FqName("dev.shustoff.dikt.ModuleScopes")

    fun getUsedModules(descriptor: IrAnnotationContainer): List<IrType> {
        val annotation = descriptor.getAnnotation(useModulesAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
    }

    fun getModuleScopes(descriptor: IrClass): Set<IrType> {
        val annotation = descriptor.getAnnotation(moduleScopesAnnotation)

        return (annotation?.getValueArgument(0) as? IrVararg)
            ?.elements
            ?.mapNotNull { (it as? IrClassReference)?.classType }
            .orEmpty()
            .toSet()
    }

    fun providesMembers(descriptor: IrAnnotationContainer): Boolean {
        return descriptor.hasAnnotation(providesMembersAnnotation)
    }

    fun singletonsByConstructor(module: IrClass): Set<IrType> {
        return module.annotations
            .filter { it.isAnnotation(moduleSingletonsAnnotation) }
            .flatMap { annotation ->
                (annotation.getValueArgument(0) as? IrVararg)
                    ?.elements
                    ?.mapNotNull { (it as? IrClassReference)?.classType }
                    .orEmpty()
            }
            .mapNotNull { it.classOrNull?.defaultType }
            .toSet()
    }

    fun getProvidedByConstructor(descriptor: IrAnnotationContainer): List<IrType> {
        return descriptor.annotations.filter { it.isAnnotation(injectByConstructorsAnnotation) }
            .flatMap { annotation ->
                (annotation.getValueArgument(0) as? IrVararg)
                    ?.elements
                    ?.mapNotNull { (it as? IrClassReference)?.classType }
                    .orEmpty()
            }
            .mapNotNull { it.classOrNull?.defaultType }
    }

    fun isInjectable(type: IrType): Boolean {
        return type.superTypes().any { it.classFqName == injectable }
    }

    fun isInjectableSingletonInScopes(type: IrType, scopes: Set<IrType>): Boolean {
        val singletonParent = type.classOrNull?.superTypes()
            ?.firstOrNull { it.classFqName == injectableSingle } as? IrSimpleType
        val scope = singletonParent?.arguments?.firstOrNull()?.typeOrNull ?: return false

        return scope in scopes
    }
}