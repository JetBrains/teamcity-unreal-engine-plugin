package com.jetbrains.teamcity.plugins.framework.resource.location

import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.jetbrains.teamcity.plugins.framework.common.ensure
import com.jetbrains.teamcity.plugins.framework.common.raise
import java.io.Reader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.reader

interface FileSystem {
    fun pathOf(fileName: String): Path
}

context(_: Raise<ResourceLocationResult.Error>, fileSystem: FileSystem)
internal fun readFile(fileName: String): Reader {
    lateinit var path: Path

    catch({
        path = fileSystem.pathOf(fileName)
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
