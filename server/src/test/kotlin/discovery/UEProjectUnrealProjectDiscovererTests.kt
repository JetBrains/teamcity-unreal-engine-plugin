package discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetPlatform
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UprojectFileDiscoverer
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.util.browser.Element
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class UEProjectUnrealProjectDiscovererTests {
    private val discoverer = UprojectFileDiscoverer()
    private val childElementMock = mockk<Element>()
    private val parentElementMock =
        mockk<Element> {
            every { children } returns listOf(childElementMock)
        }

    companion object {
        @JvmStatic
        fun generateHappyPathTestCases(): Collection<HappyPathTestCase> =
            listOf(
                HappyPathTestCase(
                    "5.1",
                    listOf(UnrealTargetPlatform.IOS, UnrealTargetPlatform.TVOS),
                    """
                    {
                        "FileVersion": 3,
                        "EngineAssociation": "5.1",
                        "Category": "",
                        "Description": "",
                        "Modules": [],
                        "Plugins": [],
                        "TargetPlatforms": [${listOf(
                        UnrealTargetPlatform.IOS.value,
                        UnrealTargetPlatform.TVOS.value,
                    ).joinToString { "\"${it}\"" }}]
                    }
                    """.trimIndent(),
                ),
                HappyPathTestCase(
                    "4.2",
                    listOf(),
                    """
                    {
                        "FileVersion": 3,
                        "EngineAssociation": "4.2",
                        "Category": "",
                        "Description": "",
                        "Modules": [],
                        "Plugins": []
                    }
                    """.trimIndent(),
                ),
                HappyPathTestCase(
                    null,
                    listOf(),
                    """
                    {
                        "FileVersion": 3,
                        "Description": "",
                        "Modules": [],
                        "Plugins": []
                    }
                    """.trimIndent(),
                ),
            )
    }

    data class HappyPathTestCase(
        val engineVersion: String?,
        val targetPlatforms: Collection<UnrealTargetPlatform>,
        val uprojectFileContent: String,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should correctly discover the project`(testCase: HappyPathTestCase) {
        // arrange
        every { childElementMock.isContentAvailable } returns true
        every { childElementMock.fullName } returns "foo.uproject"
        every { childElementMock.inputStream } returns testCase.uprojectFileContent.byteInputStream()

        // act
        val result = discoverer.discover(parentElementMock)

        // assert
        assertEquals(1, result.size)
        val discoveredProject = result.single()
        assertEquals(
            UnrealEngineProject(
                UnrealProjectPath(childElementMock.fullName),
                if (testCase.engineVersion != null) UnrealEngineIdentifier(testCase.engineVersion) else null,
                testCase.targetPlatforms,
            ),
            discoveredProject,
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "{}",
            """
            {
                "FileVersion": 3,
                "EngineAssociation": 123,
                "TargetPlatforms": []
            }
        """,
            """
            {
                "FileVersion": 3,
                "EngineAssociation": "4.1",
                "TargetPlatforms": ["FooBar"]
            }
        """,
        ],
    )
    fun `should not discover anything if the project json file is corrupted`(content: String) {
        // arrange
        every { childElementMock.isContentAvailable } returns true
        every { childElementMock.fullName } returns "foo.ueproject"
        every { childElementMock.inputStream } returns content.trimIndent().byteInputStream()

        // act
        val result = discoverer.discover(parentElementMock)

        // assert
        assertEquals(0, result.size)
    }
}
