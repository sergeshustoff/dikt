package dev.shustoff.dikt.incremental

import dev.shustoff.dikt.core.VisibilityChecker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*

class LookupHelper(
    private val lookupTracker: LookupTracker,
) {
    fun recordFullSignatureDependency(
        from: IrDeclaration,
        clazz: IrClass,
        visibilityChecker: VisibilityChecker,
        path: String
    ) {
        val position = if (lookupTracker.requiresPosition) {
            Position(
                from.fileEntry.getLineNumber(from.startOffset),
                from.fileEntry.getColumnNumber(from.startOffset)
            )
        } else {
            Position.NO_POSITION
        }
        // use all visible items for dependency, because it's the only way to
        (clazz.functions.filter { visibilityChecker.isVisible(it) } +
                clazz.properties.filter { visibilityChecker.isVisible(it) })
            .map { it.name.asString() }
            .toSet()
            .forEach { name ->
                lookupTracker.record(path, position, clazz.kotlinFqName.asString(), ScopeKind.CLASSIFIER, name)
            }
    }

    fun recordConstructorDependency(
        from: IrDeclaration,
        type: IrType,
        path: String = from.file.path
    ) {
        val clazz = type.classOrNull?.owner ?: return
        recordConstructorDependency(from, clazz, path)
    }

    fun recordConstructorDependency(
        from: IrDeclaration,
        clazz: IrClass,
        path: String = from.file.path
    ) {
        clazz.packageFqName?.let { packageFqName ->
            val position = if (lookupTracker.requiresPosition) {
                Position(
                    from.fileEntry.getLineNumber(from.startOffset),
                    from.fileEntry.getColumnNumber(from.startOffset)
                )
            } else {
                Position.NO_POSITION
            }
            //TODO: check incremental compilation when depend on class without package or nested class
            lookupTracker.record(
                path,
                position,
                packageFqName.asString(),
                ScopeKind.PACKAGE,
                clazz.name.asString()
            )
        }
    }
}