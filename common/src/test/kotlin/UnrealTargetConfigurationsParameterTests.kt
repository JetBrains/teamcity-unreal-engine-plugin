
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UnrealTargetConfigurationsParameterTests {
    data class TestCase(
        val properties: Map<String, String>,
        val propertyName: String,
        val expectedConfigurations: List<UnrealTargetConfiguration>,
    )

    companion object {
        @JvmStatic
        fun generateHappyPathTestCases() = listOf(
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to UnrealTargetConfigurationsParameter
                        .joinConfigurations(UnrealTargetConfiguration.knownConfigurations),
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                UnrealTargetConfiguration.knownConfigurations.toList(),
            ),
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to UnrealTargetConfiguration.Development.value,
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                listOf(UnrealTargetConfiguration.Development),
            ),
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to "%parameterRef%",
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                listOf(UnrealTargetConfiguration("%parameterRef%")),
            ),
        )

        @JvmStatic
        fun generateInvalidConfigurationsTestCases() = listOf(
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
                    UnrealTargetConfigurationsParameter.Client.name to listOf("foo", "bar")
                        .joinToString(separator = "+"),
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                emptyList(),
            ),
            TestCase(
                mapOf(
                    UnrealTargetConfigurationsParameter.Client.name to listOf(
                        UnrealTargetConfiguration.Development,
                        UnrealTargetConfiguration.Test,
                    ).joinToString(separator = ","),
                ),
                UnrealTargetConfigurationsParameter.Client.name,
                emptyList(),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should parse target configurations correctly`(case: TestCase) {
        // act
        val result = either {
            UnrealTargetConfigurationsParameter.parseTargetConfigurations(case.properties, case.propertyName)
        }

        // assert
        val configurations = result.getOrNull()
        assertNotNull(configurations)
        assertEquals(case.expectedConfigurations, configurations)
    }

    @ParameterizedTest
    @MethodSource("generateInvalidConfigurationsTestCases")
    fun `should raise error while parsing incorrect configurations`(case: TestCase) {
        // act
        val result = either {
            UnrealTargetConfigurationsParameter.parseTargetConfigurations(case.properties, case.propertyName)
        }

        // assert
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(case.propertyName, error.propertyName)
    }
}
