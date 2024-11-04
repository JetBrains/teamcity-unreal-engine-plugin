package discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UprojectDirsFileDiscoverer
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UprojectFileDiscoverer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.util.browser.Element
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class UProjectDirsUnrealProjectDiscovererTests {
    private val ueProjectDiscoverer = mockk<UprojectFileDiscoverer>()
    private val uprojectDirsElement = mockk<Element>()
    private val directoryElement = mockk<Element>()

    @BeforeEach
    fun init() {
        clearAllMocks()

        with(uprojectDirsElement) {
            every { isContentAvailable } returns true
            every { fullName } returns "foo.uprojectdirs"
        }

        with(directoryElement) {
            every { children } returns listOf(uprojectDirsElement)
        }
    }

    @Test
    fun `discovers unreal projects`() {
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
        every { directoryElement.browser } returns
            mockk {
                every { getElement(firstProjectPath) } returns firstProjectElement
                every { getElement(secondProjectPath) } returns secondProjectElement
            }
        val firstExpectedProject = UnrealEngineProject(UnrealProjectPath(""), UnrealEngineIdentifier("foo"), emptyList())
        val secondExpectedProject = UnrealEngineProject(UnrealProjectPath(""), UnrealEngineIdentifier("foo"), emptyList())
        every { ueProjectDiscoverer.discover(firstProjectElement) } returns listOf(firstExpectedProject)
        every { ueProjectDiscoverer.discover(secondProjectElement) } returns listOf(secondExpectedProject)
        val uProjectDirsDiscoverer = UprojectDirsFileDiscoverer(ueProjectDiscoverer)

        // act
        val result = uProjectDirsDiscoverer.discover(directoryElement)

        // assert
        result shouldHaveSize 2
        result.shouldContainExactly(
            firstExpectedProject,
            secondExpectedProject,
        )
    }

    @Test
    fun `discovers a single Unreal project when there are duplications of them`() {
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
        every { directoryElement.browser } returns
            mockk {
                every { getElement(projectPath) } returns projectElement
            }
        every {
            ueProjectDiscoverer.discover(projectElement)
        } returns listOf(UnrealEngineProject(UnrealProjectPath(""), UnrealEngineIdentifier("foo"), emptyList()))
        val uProjectDirsDiscoverer = UprojectDirsFileDiscoverer(ueProjectDiscoverer)

        // act
        val result = uProjectDirsDiscoverer.discover(directoryElement)

        // assert
        result shouldHaveSize 1
    }

    @Test
    fun `ignores unknown paths during discovery`() {
        // arrange
        setUprojectDirsFileContent(
            """
            foo
            bar
        """,
        )
        every { directoryElement.browser } returns
            mockk {
                every { getElement(any()) } returns null
            }
        val uProjectDirsDiscoverer = UprojectDirsFileDiscoverer(ueProjectDiscoverer)

        // act
        val result = uProjectDirsDiscoverer.discover(directoryElement)

        // assert
        result shouldHaveSize 0
    }

    private fun setUprojectDirsFileContent(value: String) {
        every { uprojectDirsElement.inputStream } returns value.trimIndent().byteInputStream()
    }
}
