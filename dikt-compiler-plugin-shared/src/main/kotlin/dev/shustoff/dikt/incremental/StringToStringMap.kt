package dev.shustoff.dikt.incremental

import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.name.FqName
import java.io.File

class ClassToPathMap(storageFile: File) : BasicStringMap<String>(storageFile, PathStringDescriptor) {
    override fun dumpValue(value: String): String = value

    operator fun get(key: FqName): String? = storage[key.asString()]

    @Synchronized
    operator fun set(key: FqName, values: String?) {
        if (values == null) {
            storage.remove(key.asString())
            return
        }

        storage[key.asString()] = values
    }
}