package dev.shustoff.dikt.incremental

import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.name.FqName

class LookupHelper(
    private val lookupTracker: LookupTracker,
) {
    fun recordLookup(
        from: IrClass,
        toFqName: FqName
    ) {
        // ScopeKind is not actually used inside, so why bother
        lookupTracker.record(from.file.path, getPosition(from), toFqName.parent().asString(), ScopeKind.CLASSIFIER, toFqName.shortName().asString())
    }

    private fun getPosition(from: IrClass) = if (lookupTracker.requiresPosition) {
        Position(
            from.fileEntry.getLineNumber(from.startOffset),
            from.fileEntry.getColumnNumber(from.startOffset),
        )
    } else {
        Position.NO_POSITION
    }
}