package dev.shustoff.dikt.compiler

import org.jetbrains.kotlin.codegen.isJvmStaticInObjectOrClassOrInterface
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor

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

        private fun isProvidedByDi(descriptor: CallableMemberDescriptor): Boolean {
            if (descriptor is FunctionDescriptor) {
                return descriptor.annotations.hasAnnotation(createAnnotation)
                        || descriptor.annotations.hasAnnotation(providedAnnotation)
                        || (descriptor.annotations.hasAnnotation(cachedAnnotation)
                        && !descriptor.isJvmStaticInObjectOrClassOrInterface()
                        && descriptor.parents.any { it is ClassDescriptor })
            }
            return false
        }
    }
}