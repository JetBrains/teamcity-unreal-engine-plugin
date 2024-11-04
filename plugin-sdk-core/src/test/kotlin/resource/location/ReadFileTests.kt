package resource.location

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.resource.location.FileSystem
import com.jetbrains.teamcity.plugins.framework.resource.location.readFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class ReadFileTests {
    @Test
    fun `should raise an error when the provided path is invalid`() {
        val context = object: FileSystem {
            override fun pathOf(fileName: String): Path {
                throw InvalidPathException("", fileName)
            }
        }

        val result = with(context) { either { readFile("foo") } }

        val error = result.leftOrNull()
        assertNotNull(error)
        assertIs<InvalidPathException>(error.exception)
    }

    @Test
    fun `should raise an error when the file does not exist`() {
        val context = object: FileSystem {
            override fun pathOf(fileName: String) = mockk<Path>()
        }
        mockkStatic(Files::exists)
        every { Files.exists(any(), *anyVararg<LinkOption>()) } returns false

        val result = with(context) { either { readFile("foo") } }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should raise an error when the requested path is a directory`() {
        val context = object: FileSystem {
            override fun pathOf(fileName: String) = mockk<Path>()
        }
        mockkStatic(Files::exists)
        every { Files.exists(any(), *anyVararg<LinkOption>()) } returns true
        every { Files.isDirectory(any(), *anyVararg<LinkOption>()) } returns true

        val result = with(context) { either { readFile("foo") } }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should raise an error when the reader is unavailable for any reason`() {
        val context = object: FileSystem {
            override fun pathOf(fileName: String) = mockk<Path>()
        }
        mockkStatic(Files::exists)
        every { Files.exists(any(), *anyVararg<LinkOption>()) } returns true
        every { Files.isDirectory(any(), *anyVararg<LinkOption>()) } returns false
        every { Files.newInputStream(any(), *anyVararg<OpenOption>()) } throws IOException()

        val result = with(context) { either { readFile("foo") } }

        val error = result.leftOrNull()
        assertNotNull(error)
        assertIs<IOException>(error.exception)
    }

    @Test
    fun `should return a reader`() {
        val context = object: FileSystem {
            override fun pathOf(fileName: String) = mockk<Path>()
        }
        mockkStatic(Files::exists)
        every { Files.exists(any(), *anyVararg<LinkOption>()) } returns true
        every { Files.isDirectory(any(), *anyVararg<LinkOption>()) } returns false
        val content = """
            foo
            bar
        """.trimIndent()
        every { Files.newInputStream(any(), *anyVararg<OpenOption>()) } returns ByteArrayInputStream(content.toByteArray())

        val result = with(context) { either { readFile("foo") } }

        val reader = result.getOrNull()
        assertNotNull(reader)
        assertEquals(reader.readText(), content)
    }

    @AfterTest
    fun removeMocks() {
        unmockkAll()
    }
}
