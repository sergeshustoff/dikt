package dev.shustoff.dikt.compiler

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor

class DiktPlatformDiagnosticSuppressor : PlatformDiagnosticSuppressor {
    override fun shouldReportNoBody(descriptor: CallableMemberDescriptor): Boolean {
        return !isProvidedByDi(descriptor)
    }

    override fun shouldReportUnusedParameter(parameter: VariableDescriptor): Boolean {
        return true
    }


    companion object {
        private val createAnnotation = FqName("dev.shustoff.dikt.Create")
        private val cachedAnnotation = FqName("dev.shustoff.dikt.CreateCached")
        private val providedAnnotation = FqName("dev.shustoff.dikt.Provided")
        private val moduleAnnotation = FqName("dev.shustoff.dikt.DiModule")

        private fun isProvidedByDi(descriptor: CallableMemberDescriptor): Boolean {
            val containingDeclaration = descriptor.containingDeclaration
            return descriptor is FunctionDescriptor
                    && (descriptor.annotations.hasAnnotation(createAnnotation)
                    || descriptor.annotations.hasAnnotation(cachedAnnotation)
                    || descriptor.annotations.hasAnnotation(providedAnnotation))
                    && containingDeclaration is ClassDescriptor
                    && containingDeclaration.annotations.hasAnnotation(moduleAnnotation)
        }
    }
}