package build.status.ugs

import arrow.core.NonEmptyList
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBadgeName
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBuildFeatureParameters
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsParametersParser
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsProject
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UgsParametersParserTests {
    private val serverUrlIsMissing =
        PropertyValidationError("ugs-metadata-server-server-url", "The metadata server URL cannot be empty")
    private val badgeNameIsMissing = PropertyValidationError("ugs-metadata-server-badge-name", "The badge name cannot be empty")
    private val projectPathIsMissing = PropertyValidationError("ugs-metadata-server-project-path", "The project path cannot be empty")

    data class HappyPathTestCase(
        val parameters: Map<String, String>,
        val parsedParameters: UgsBuildFeatureParameters?,
    )

    private fun `correctly parses parameters`(): Collection<HappyPathTestCase> =
        listOf(
            HappyPathTestCase(
                mapOf(
                    "ugs-metadata-server-server-url" to "http://localhost:1111",
                    "ugs-metadata-server-badge-name" to "foo",
                    "ugs-metadata-server-project-path" to "//depot/stream/project",
                ),
                UgsBuildFeatureParameters(
                    UgsMetadataServerUrl("http://localhost:1111"),
                    UgsBadgeName("foo"),
                    UgsProject("//depot/stream/project"),
                ),
            ),
        )

    @ParameterizedTest
    @MethodSource("correctly parses parameters")
    fun `correctly parses parameters`(case: HappyPathTestCase) {
        // act
        val result = UgsParametersParser().parse(case.parameters).getOrNull()

        // assert
        assertNotNull(result)
        assertEquals(case.parsedParameters, result)
    }

    data class WrongParametersTestCase(
        val parameters: Map<String, String>,
        val errors: NonEmptyList<PropertyValidationError>,
    )

    private fun `raises error when parameters are invalid`(): Collection<WrongParametersTestCase> =
        listOf(
            WrongParametersTestCase(
                mapOf(
                    serverUrlIsMissing.propertyName to "http://localhost:1111",
                ),
                NonEmptyList(
                    badgeNameIsMissing,
                    listOf(projectPathIsMissing),
                ),
            ),
            WrongParametersTestCase(
                mapOf(
                    projectPathIsMissing.propertyName to "//foo/bar/x",
                    badgeNameIsMissing.propertyName to "foo,",
                ),
                NonEmptyList(
                    serverUrlIsMissing,
                    emptyList(),
                ),
            ),
        )

    @ParameterizedTest
    @MethodSource("raises error when parameters are invalid")
    fun `raises error when parameters are invalid`(case: WrongParametersTestCase) {
        // act
        val result = UgsParametersParser().parse(case.parameters).leftOrNull()

        // assert
        assertNotNull(result)
        result.shouldContainExactlyInAnyOrder(case.errors)
    }
}
