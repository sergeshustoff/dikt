package dev.shustoff.dikt.incremental

import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import org.jetbrains.kotlin.name.FqName
import java.io.File

class ClassToStringsListMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
    override fun dumpValue(value: Collection<String>): String = value.dumpCollection()

    @Synchronized
    fun add(key: FqName, value: String) {
        storage.append(key.asString(), listOf(value))
    }

    operator fun get(key: FqName): Collection<String> =
        storage[key.asString()] ?: setOf()

    @Synchronized
    operator fun set(key: FqName, values: Collection<String>) {
        if (values.isEmpty()) {
            remove(key)
            return
        }

        storage[key.asString()] = values
    }

    @Synchronized
    fun remove(key: FqName) {
        storage.remove(key.asString())
    }

    @Synchronized
    fun removeValues(key: FqName, removed: Set<String>) {
        val notRemoved = this[key].filter { it !in removed }
        this[key] = notRemoved
    }
}