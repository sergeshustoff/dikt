package dev.shustoff.dikt.test

import dev.shustoff.dikt.Injectable

class SomeInjectable(
    val dependency: Dependency,
    val name: String
) : Injectable