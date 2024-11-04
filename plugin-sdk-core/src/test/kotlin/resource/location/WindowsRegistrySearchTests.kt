package resource.location

import arrow.core.raise.either
import com.intellij.execution.ExecutionException
import com.jetbrains.teamcity.plugins.framework.common.CommandLineRunner
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.WindowsResourceLocationContext
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistryEntry
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistrySearchFilter
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistryValueType
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.windowsRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WindowsRegistrySearchTests {
    private val commandLineRunnerMock = mockk<CommandLineRunner>()
    private val defaultContext = object : WindowsResourceLocationContext {
        override val commandLineRunner = commandLineRunnerMock
        override fun pathOf(fileName: String) = Path.of(fileName)
    }
    private val defaultFilter = object : WindowsRegistrySearchFilter {
        override fun accept(key: WindowsRegistryEntry.Key) = true
        override fun accept(value: WindowsRegistryEntry.Value) = true
    }

    @Test
    fun `should raise an error when there is an error in an underlying command`() {
        every { commandLineRunnerMock.run(any()) } throws ExecutionException("")

        val result = with(defaultContext) { either { windowsRegistry("", defaultFilter) } }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should raise an error when there is no result in an underlying command`() {
        every { commandLineRunnerMock.run(any()) } returns null

        val result = with(defaultContext) { either { windowsRegistry("", defaultFilter) } }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should raise an error when an underlying command exited with a non-zero exit code`() {
        every { commandLineRunnerMock.run(any()) } returns CommandLineRunner.RunResult(-1, emptyList(), emptyList())

        val result = with(defaultContext) { either { windowsRegistry("", defaultFilter) } }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should return entries matching the given filter`() {
        val expectedEntry = WindowsRegistryEntry.Value("foo", WindowsRegistryValueType.Str, "bar")
        val filter = object : WindowsRegistrySearchFilter {
            override fun accept(key: WindowsRegistryEntry.Key) = false
            override fun accept(value: WindowsRegistryEntry.Value): Boolean = value.name == expectedEntry.name
        }
        every { commandLineRunnerMock.run(any()) } returns CommandLineRunner.RunResult(0,
            listOf(
                "HKEY_CURRENT_USER\\Software\\Epic Games\\Unreal Engine\\Builds",
                "\t${expectedEntry.name}\t${expectedEntry.type.id}\t${expectedEntry.data}",
                "\tbar\t${WindowsRegistryValueType.Int}\t1",
            ), emptyList())

        val result = with(defaultContext) { either { windowsRegistry("", filter) } }

        val entries = result.getOrNull()
        assertNotNull(entries)
        assertEquals(1, entries.size)
        assertEquals(expectedEntry, entries.single())
    }

    @AfterTest
    fun removeMocks() {
        unmockkAll()
    }
}
