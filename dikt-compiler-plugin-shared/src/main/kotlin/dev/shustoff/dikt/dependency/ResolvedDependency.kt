package dev.shustoff.dikt.dependency

class ResolvedDependency(
    val dependency: Dependency,
    val params: List<ResolvedDependency> = emptyList()
)