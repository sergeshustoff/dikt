package dev.shustoff.dikt.compiler

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.resolve.source.getPsi

val IrDeclaration.psiElementSafe: PsiElement? get() {
    val source = (this as? DeclarationDescriptorWithSource)?.source
    return source?.getPsi()
}