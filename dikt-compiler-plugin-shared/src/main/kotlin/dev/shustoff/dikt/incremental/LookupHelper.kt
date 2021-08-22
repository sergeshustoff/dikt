package dev.shustoff.dikt.incremental

import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.FqName

class LookupHelper(
    private val lookupTracker: LookupTracker,
) {
    fun recordLookup(
        fromPath: String,
        toFqName: FqName
    ) {
        // ScopeKind is not actually used inside, so why bother
        lookupTracker.record(fromPath, getPosition(), toFqName.parent().asString(), ScopeKind.CLASSIFIER, toFqName.shortName().asString())
    }

    private fun getPosition() = if (lookupTracker.requiresPosition) {
        ZERO_POSITION //TODO:later make sure position is not necessary
    } else {
        Position.NO_POSITION
    }

    companion object {
        private val ZERO_POSITION = Position(0, 0)
    }
}