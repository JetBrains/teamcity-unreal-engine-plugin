package discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetPlatform
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UprojectFileDiscoverer
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.util.browser.Element
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class UEProjectUnrealProjectDiscovererTests {
    private val discoverer = UprojectFileDiscoverer()
    private val childElement = mockk<Element>()
    private val parentElement = mockk<Element>()

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { parentElement.children } returns listOf(childElement)
    }

    data class HappyPathTestCase(
        val engineVersion: String?,
        val targetPlatforms: Collection<UnrealTargetPlatform>,
        val uprojectFileContent: String,
    )

    private fun `discovers the project`(): Collection<HappyPathTestCase> =
        listOf(
            HappyPathTestCase(
                engineVersion = "5.1",
                targetPlatforms = listOf(UnrealTargetPlatform.IOS, UnrealTargetPlatform.TVOS),
                uprojectFileContent =
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
                engineVersion = "4.2",
                targetPlatforms = listOf(),
                uprojectFileContent =
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
                engineVersion = null,
                targetPlatforms = listOf(),
                uprojectFileContent =
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

    @ParameterizedTest
    @MethodSource("discovers the project")
    fun `discovers the project`(testCase: HappyPathTestCase) {
        // arrange
        every { childElement.isContentAvailable } returns true
        every { childElement.fullName } returns "foo.uproject"
        every { childElement.inputStream } returns testCase.uprojectFileContent.byteInputStream()

        // act
        val result = discoverer.discover(parentElement)

        // assert
        result shouldHaveSingleElement
            UnrealEngineProject(
                UnrealProjectPath(childElement.fullName),
                if (testCase.engineVersion != null) UnrealEngineIdentifier(testCase.engineVersion) else null,
                testCase.targetPlatforms,
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
    fun `does not discover anything if the project json file is corrupted`(content: String) {
        // arrange
        every { childElement.isContentAvailable } returns true
        every { childElement.fullName } returns "foo.ueproject"
        every { childElement.inputStream } returns content.trimIndent().byteInputStream()

        // act
        val result = discoverer.discover(parentElement)

        // assert
        result shouldHaveSize 0
    }
}
