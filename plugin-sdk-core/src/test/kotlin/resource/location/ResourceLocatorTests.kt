package resource.location

import com.jetbrains.teamcity.plugins.framework.common.CommandLineRunner
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.framework.resource.location.QueryBuilder
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationContext
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocator
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.map
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class ResourceLocatorTests {
    @Test
    fun `should only return results for MacOS when requested`() = runTest {
        val locator = createLocator(createEnvironment(OSType.MacOs))

        val result = locator.locateResources { buildQuery() }

        assertEquals(1, result.size)
        val locationResult = result[0]
        assertIs<ResourceLocationResult.Success<OSType>>(locationResult)
        assertEquals(OSType.MacOs, locationResult.data)
    }

    @Test
    fun `should only return results for Windows when requested`() = runTest {
        val locator = createLocator(createEnvironment(OSType.Windows))

        val result = locator.locateResources { buildQuery() }

        assertEquals(1, result.size)
        val locationResult = result[0]
        assertIs<ResourceLocationResult.Success<OSType>>(locationResult)
        assertEquals(OSType.Windows, locationResult.data)
    }

    @Test
    fun `should only return results for Linux when requested`() = runTest {
        val locator = createLocator(createEnvironment(OSType.Linux))

        val result = locator.locateResources { buildQuery() }

        assertEquals(1, result.size)
        val locationResult = result[0]
        assertIs<ResourceLocationResult.Success<OSType>>(locationResult)
        assertEquals(OSType.Linux, locationResult.data)
    }

    private fun createEnvironment(os: OSType): Environment = object : Environment {
        override val osType = os
        override val homeDirectory = Path.of("")
        override val programDataDirectory = Path.of("")
        override fun getEnvironmentVariable(name: String) = null
    }

    private fun createLocator(environment: Environment): ResourceLocator {
        mockkStatic(Files::exists)
        every { Files.exists(any(), *anyVararg<LinkOption>()) } returns true
        every { Files.isDirectory(any(), *anyVararg<LinkOption>()) } returns false

        val content = environment.osType.toString()
        every {
            Files.newInputStream(
                any(),
                *anyVararg<OpenOption>()
            )
        } returns ByteArrayInputStream(content.toByteArray())

        return ResourceLocator(
            environment,
            mockk<ResourceLocationContext> {
                every { pathOf(any()) } returns mockk<Path>()
                every { commandLineRunner } returns CommandLineRunner()
            },
        )
    }

    private fun QueryBuilder<OSType>.buildQuery() {
        windows({ file("foo").map { OSType.valueOf(it.readText()) } })
        linux({ file("foo").map { OSType.valueOf(it.readText()) } })
        macos({ file("foo").map { OSType.valueOf(it.readText()) } })
    }

    @AfterTest
    fun removeMocks() {
        unmockkAll()
    }
}
