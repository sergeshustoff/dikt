package dev.shustoff.dikt.dependency

data class ResolvedDependency(
    val dependency: Dependency,
    val nestedModulesChain: ResolvedDependency? = null,
    val params: List<ResolvedDependency> = emptyList(),
    val extensionParam: ResolvedDependency? = null
)