package com.jetbrains.teamcity.plugins.framework.resource.location

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.ensure
import java.io.Reader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.reader

interface FileSystem {
    fun pathOf(fileName: String): Path
}

context(Raise<ResourceLocationResult.Error>, FileSystem)
internal fun readFile(fileName: String): Reader {
    lateinit var path: Path

    catch({
        path = pathOf(fileName)
    }) {
        raise(ResourceLocationResult.Error("Path $fileName is invalid", it))
    }

    ensure(path.exists()) { ResourceLocationResult.Error("Path $fileName not found") }
    ensure(!path.isDirectory()) { ResourceLocationResult.Error("Path $fileName is not a file path") }

    catch({
        return path.reader().buffered()
    }) {
        raise(ResourceLocationResult.Error("Unknown error occurred during read of the file $fileName", it))
    }
}
