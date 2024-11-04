package discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UnrealEngineRunnerDiscoverer
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UnrealProjectDiscoverer
import com.jetbrains.teamcity.plugins.unrealengine.server.runner.UnrealEngineRunnerParametersProvider
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject
import jetbrains.buildServer.util.browser.Browser
import jetbrains.buildServer.util.browser.Element
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UnrealEngineRunnerDiscovererTests {
    private val underlyingDiscoverer = mockk<UnrealProjectDiscoverer>()
    private val defaultProperties = mockk<UnrealEngineRunnerParametersProvider>()
    private val buildTypeSettings = mockk<BuildTypeSettings>()
    private val browser = mockk<Browser>()
    private val element = mockk<Element>()

    @BeforeEach
    fun init() {
        clearAllMocks()

        with(element) {
            every { children } returns emptyList()
            every { fullName } returns "foo"
        }

        every { browser.root } returns element
    }

    data class TestCase(
        val discoveredProjects: Collection<UnrealEngineProject>,
        val defaultProperties: Map<String, String>,
        val expectedDiscoveredObjects: Collection<DiscoveredObject>,
    )

    private fun `discovers Unreal runner`(): Collection<TestCase> {
        val defaultAndDiscoveredPropertiesIntersection =
            TestCase(
                discoveredProjects =
                    listOf(
                        UnrealEngineProject(UnrealProjectPath("discovered.uproject"), null, listOf()),
                    ),
                defaultProperties =
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "default",
                    ),
                expectedDiscoveredObjects =
                    listOf(
                        DiscoveredObject(
                            UnrealEngineRunner.RUN_TYPE,
                            mapOf(BuildCookRunProjectPathParameter.name to "discovered.uproject"),
                        ),
                    ),
            )

        val duplicatedProjects =
            TestCase(
                discoveredProjects =
                    listOf(
                        UnrealEngineProject(UnrealProjectPath("foo.uproject"), null, listOf()),
                        UnrealEngineProject(UnrealProjectPath("foo.uproject"), null, listOf()),
                    ),
                defaultProperties = emptyMap(),
                expectedDiscoveredObjects =
                    listOf(
                        DiscoveredObject(
                            UnrealEngineRunner.RUN_TYPE,
                            mapOf(BuildCookRunProjectPathParameter.name to "foo.uproject"),
                        ),
                    ),
            )

        return listOf(
            defaultAndDiscoveredPropertiesIntersection,
            duplicatedProjects,
        )
    }

    @ParameterizedTest
    @MethodSource("discovers Unreal runner")
    fun `discovers Unreal runner`(case: TestCase) {
        // arrange
        every { underlyingDiscoverer.discover(any()) } returns case.discoveredProjects
        every { defaultProperties.getDefaultValues() } returns case.defaultProperties
        val discoverer = UnrealEngineRunnerDiscoverer(listOf(underlyingDiscoverer), defaultProperties)

        // act
        val result = discoverer.discover(buildTypeSettings, browser)

        // assert
        result?.shouldBe(case.expectedDiscoveredObjects)
    }
}
