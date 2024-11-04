
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UnrealTargetConfigurationsParameterTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val propertyName: String,
        val expectedConfigurations: List<UnrealTargetConfiguration>,
    )

    private fun `parses target configurations correctly`() =
        listOf(
            TestCase(
                runnerParameters =
                    mapOf(
                        UnrealTargetConfigurationsParameter.Client.name to
                            UnrealTargetConfigurationsParameter
                                .joinConfigurations(UnrealTargetConfiguration.knownConfigurations),
                    ),
                propertyName = UnrealTargetConfigurationsParameter.Client.name,
                expectedConfigurations = UnrealTargetConfiguration.knownConfigurations.toList(),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        UnrealTargetConfigurationsParameter.Client.name to UnrealTargetConfiguration.Development.value,
                    ),
                propertyName = UnrealTargetConfigurationsParameter.Client.name,
                expectedConfigurations = listOf(UnrealTargetConfiguration.Development),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        UnrealTargetConfigurationsParameter.Client.name to "%parameterRef%",
                    ),
                propertyName = UnrealTargetConfigurationsParameter.Client.name,
                expectedConfigurations = listOf(UnrealTargetConfiguration("%parameterRef%")),
            ),
        )

    @ParameterizedTest
    @MethodSource("parses target configurations correctly")
    fun `parses target configurations correctly`(case: TestCase) {
        // act
        val result =
            either {
                UnrealTargetConfigurationsParameter.parseTargetConfigurations(case.runnerParameters, case.propertyName)
            }

        // assert
        val configurations = result.getOrNull()
        configurations shouldBe case.expectedConfigurations
    }

    private fun `raises error while parsing incorrect configurations`() =
        listOf(
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to "",
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                emptyList(),
            ),
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to "foo",
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                emptyList(),
            ),
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to
                        listOf("foo", "bar")
                            .joinToString(separator = "+"),
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                emptyList(),
            ),
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to
                        listOf(
                            UnrealTargetConfiguration.Development,
                            UnrealTargetConfiguration.Test,
                        ).joinToString(separator = ","),
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                emptyList(),
            ),
        )

    @ParameterizedTest
    @MethodSource("raises error while parsing incorrect configurations")
    fun `raises error while parsing incorrect configurations`(case: TestCase) {
        // act
        val result =
            either {
                UnrealTargetConfigurationsParameter.parseTargetConfigurations(case.runnerParameters, case.propertyName)
            }

        // assert
        val error = result.leftOrNull()
        error shouldNotBe null
        error!!.propertyName shouldBe case.propertyName
    }
}
