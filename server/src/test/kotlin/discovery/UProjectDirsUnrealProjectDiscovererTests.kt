package discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UprojectDirsFileDiscoverer
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UprojectFileDiscoverer
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.util.browser.Element
import kotlin.test.Test
import kotlin.test.assertEquals

class UProjectDirsUnrealProjectDiscovererTests {
    private val ueProjectDiscovererMock = mockk<UprojectFileDiscoverer>()
    private val uprojectDirsElementMock = mockk<Element> {
        every { isContentAvailable } returns true
        every { fullName } returns "foo.uprojectdirs"
    }
    private val directoryElementMock = mockk<Element> {
        every { children } returns listOf(uprojectDirsElementMock)
    }

    @Test
    fun `should correctly discover unreal projects`() {
        // arrange
        val firstProjectPath = "project1/"
        val secondProjectPath = "subdir/project2/"
        setUprojectDirsFileContent(
            """
            ; foo comment
            $firstProjectPath
            ; bar comment
            $secondProjectPath
        """,
        )
        val firstProjectElement = mockk<Element>(relaxed = true)
        val secondProjectElement = mockk<Element>(relaxed = true)
        every { directoryElementMock.browser } returns mockk {
            every { getElement(firstProjectPath) } returns firstProjectElement
            every { getElement(secondProjectPath) } returns secondProjectElement
        }
        val firstExpectedProject = UnrealEngineProject(UnrealProjectPath(""), UnrealEngineIdentifier("foo"), emptyList())
        val secondExpectedProject = UnrealEngineProject(UnrealProjectPath(""), UnrealEngineIdentifier("foo"), emptyList())
        every { ueProjectDiscovererMock.discover(firstProjectElement) } returns listOf(firstExpectedProject)
        every { ueProjectDiscovererMock.discover(secondProjectElement) } returns listOf(secondExpectedProject)
        val uProjectDirsDiscoverer = UprojectDirsFileDiscoverer(ueProjectDiscovererMock)

        // act
        val result = uProjectDirsDiscoverer.discover(directoryElementMock)

        // assert
        assertEquals(2, result.size)
        assertEquals(firstExpectedProject, result.first())
        assertEquals(secondExpectedProject, result.last())
    }

    @Test
    fun `should discover a single Unreal project when there are duplications of them`() {
        // arrange
        val projectPath = "project/"
        setUprojectDirsFileContent(
            """
            $projectPath
            $projectPath
            $projectPath
        """,
        )
        val projectElement = mockk<Element>()
        every { directoryElementMock.browser } returns mockk {
            every { getElement(projectPath) } returns projectElement
        }
        every {
            ueProjectDiscovererMock.discover(projectElement)
        } returns listOf(UnrealEngineProject(UnrealProjectPath(""), UnrealEngineIdentifier("foo"), emptyList()))
        val uProjectDirsDiscoverer = UprojectDirsFileDiscoverer(ueProjectDiscovererMock)

        // act
        val result = uProjectDirsDiscoverer.discover(directoryElementMock)

        // assert
        assertEquals(1, result.size)
    }

    @Test
    fun `should ignore unknown paths during discovery`() {
        // arrange
        setUprojectDirsFileContent(
            """
            foo
            bar
        """,
        )
        every { directoryElementMock.browser } returns mockk {
            every { getElement(any()) } returns null
        }
        val uProjectDirsDiscoverer = UprojectDirsFileDiscoverer(ueProjectDiscovererMock)

        // act
        val result = uProjectDirsDiscoverer.discover(directoryElementMock)

        // assert
        assertEquals(0, result.size)
    }

    private fun setUprojectDirsFileContent(value: String) {
        every { uprojectDirsElementMock.inputStream } returns value.trimIndent().byteInputStream()
    }
}
