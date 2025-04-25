package buildgraph

import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraph
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphDefinitionLoader
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphNode
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphNodeGroup
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphParser
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.ValidatedSetupBuild
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.InputStream

class BuildGraphDefinitionLoaderTests {
    private val settings = BuildGraphSettings()
    private val parser = mockk<BuildGraphParser>()
    private val loader = BuildGraphDefinitionLoader(parser, settings)

    data class TestCase(
        val setupBuild: ValidatedSetupBuild,
        val expectedGraph: BuildGraph<BuildGraphNodeGroup>? = null,
        val expectedError: Error? = null,
    )

    private fun `happy path cases`(): List<TestCase> {
        val validGraph =
            BuildGraph(
                mapOf(
                    BuildGraphNodeGroup(
                        "Group1",
                        listOf("Agent1"),
                        listOf(BuildGraphNode("Node1", listOf())),
                    ) to listOf(),
                ),
                listOf(),
            )

        val validArtifact = createArtifact()
        val validBuild =
            mockk<ValidatedSetupBuild> {
                every { getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT) } returns
                    mockk {
                        every { iterateArtifacts(any()) } answers {
                            arg<BuildArtifacts.BuildArtifactsProcessor>(0).processBuildArtifact(validArtifact)
                        }
                    }
            }

        every { with(any<Raise<Error>>()) { parser.parse(any<InputStream>()) } } returns validGraph

        return listOf(
            TestCase(
                setupBuild = validBuild,
                expectedGraph = validGraph,
            ),
        )
    }

    private fun `error cases`(): List<TestCase> {
        val missingArtifactBuild =
            mockk<ValidatedSetupBuild> {
                every { getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT) } returns
                    mockk {
                        every { iterateArtifacts(any()) } just runs
                    }
            }

        val buildWithUnrelatedArtifacts =
            mockk<ValidatedSetupBuild> {
                every { getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT) } returns
                    mockk {
                        every { iterateArtifacts(any()) } answers {
                            arg<BuildArtifacts.BuildArtifactsProcessor>(0).also {
                                it.processBuildArtifact(createArtifact(name = "foo"))
                                it.processBuildArtifact(createArtifact(isFile = false))
                            }
                        }
                    }
            }

        return listOf(
            TestCase(
                setupBuild = missingArtifactBuild,
                expectedError =
                    GenericError(
                        "It appears that the build graph setup build has failed, it hasn't published the exported graph file",
                    ),
            ),
            TestCase(
                setupBuild = buildWithUnrelatedArtifacts,
                expectedError =
                    GenericError(
                        "It appears that the build graph setup build has failed, it hasn't published the exported graph file",
                    ),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("happy path cases")
    fun `loads build graph definition from artifacts`(case: TestCase) {
        // act
        val result = either { loader.loadFrom(case.setupBuild) }

        // assert
        result.isRight() shouldBe true
        result.getOrNull() shouldBe case.expectedGraph
    }

    @ParameterizedTest
    @MethodSource("error cases")
    fun `raises an error when artifact is missing`(case: TestCase) {
        // act
        val result = either { loader.loadFrom(case.setupBuild) }

        // assert
        result.isLeft() shouldBe true
        result.leftOrNull() shouldBe case.expectedError
    }

    private fun createArtifact(
        name: String = settings.graphArtifactName,
        content: String = "{}",
        isFile: Boolean = true,
    ) = mockk<BuildArtifact> {
        every { this@mockk.name } returns name
        every { inputStream } returns ByteArrayInputStream(content.toByteArray())
        every { this@mockk.isFile } returns isFile
    }
}
