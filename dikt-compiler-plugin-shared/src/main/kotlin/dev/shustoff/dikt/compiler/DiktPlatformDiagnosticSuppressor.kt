package dev.shustoff.dikt.compiler

import dev.shustoff.dikt.core.Annotations
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor

class DiktPlatformDiagnosticSuppressor : PlatformDiagnosticSuppressor {
    override fun shouldReportNoBody(descriptor: CallableMemberDescriptor): Boolean {
        return descriptor !is FunctionDescriptor || !Annotations.isProvidedByDi(descriptor)
    }

    override fun shouldReportUnusedParameter(parameter: VariableDescriptor): Boolean {
        return true
    }
}