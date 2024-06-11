package discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UnrealEngineRunnerDiscoverer
import com.jetbrains.teamcity.plugins.unrealengine.server.discovery.UnrealProjectDiscoverer
import com.jetbrains.teamcity.plugins.unrealengine.server.runner.UnrealEngineRunnerParametersProvider
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject
import jetbrains.buildServer.util.browser.Browser
import jetbrains.buildServer.util.browser.Element
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class UnrealEngineRunnerDiscovererTests {
    private val underlyingDiscovererMock = mockk<UnrealProjectDiscoverer>()
    private val defaultPropertiesProvider = mockk<UnrealEngineRunnerParametersProvider>()

    companion object {
        @JvmStatic
        fun generateHappyPathTestCases(): Collection<TestCase> {
            val defaultAndDiscoveredPropertiesIntersection =
                TestCase(
                    listOf(
                        UnrealEngineProject(UnrealProjectPath("discovered.uproject"), null, listOf()),
                    ),
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "default",
                    ),
                    listOf(
                        DiscoveredObject(
                            UnrealEngineRunner.RUN_TYPE,
                            mapOf(BuildCookRunProjectPathParameter.name to "discovered.uproject"),
                        ),
                    ),
                )

            val duplicatedProjects =
                TestCase(
                    listOf(
                        UnrealEngineProject(UnrealProjectPath("foo.uproject"), null, listOf()),
                        UnrealEngineProject(UnrealProjectPath("foo.uproject"), null, listOf()),
                    ),
                    emptyMap(),
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
    }

    data class TestCase(
        val discoveredProjects: Collection<UnrealEngineProject>,
        val defaultProperties: Map<String, String>,
        val expectedDiscoveredObjects: Collection<DiscoveredObject>,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should correctly discover Unreal runner`(case: TestCase) {
        // arrange
        every { underlyingDiscovererMock.discover(any()) } returns case.discoveredProjects
        every { defaultPropertiesProvider.getDefaultValues() } returns case.defaultProperties
        val discoverer = UnrealEngineRunnerDiscoverer(listOf(underlyingDiscovererMock), defaultPropertiesProvider)

        // act
        val result =
            discoverer.discover(
                mockk<BuildTypeSettings>(),
                mockk<Browser> {
                    every { root } returns
                        mockk<Element> {
                            every { children } returns emptyList()
                            every { fullName } returns "foo"
                        }
                },
            )

        // assert
        assertEquals(case.expectedDiscoveredObjects, result ?: emptyList())
    }
}
