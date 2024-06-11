
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookCulture
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookCulturesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookMap
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookOptions
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.MapsToCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnversionedCookedContentParameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookOptionsTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: CookOptions,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    companion object {
        @JvmStatic
        fun generateTestCases() =
            listOf(
                TestCase(
                    runnerParameters =
                        mapOf(
                            MapsToCookParameter.name to "Map1+Map2+Map3",
                            CookCulturesParameter.name to "Culture1+Culture2",
                            UnversionedCookedContentParameter.name to true.toString(),
                        ),
                    expectedOptions =
                        CookOptions(
                            mapsToCook =
                                listOf(
                                    CookMap("Map1"),
                                    CookMap("Map2"),
                                    CookMap("Map3"),
                                ),
                            cookCultures =
                                listOf(
                                    CookCulture("Culture1"),
                                    CookCulture("Culture2"),
                                ),
                            unversionedContent = true,
                        ),
                    shouldContainItems =
                        listOf(
                            "-cook",
                            "-map=Map1+Map2+Map3",
                            "-cookcultures=Culture1+Culture2",
                            "-unversionedcookedcontent",
                        ),
                    shouldNotContainItems = emptyList(),
                ),
                TestCase(
                    runnerParameters = mapOf(),
                    expectedOptions = CookOptions(),
                    shouldContainItems = listOf("-cook"),
                    shouldNotContainItems = listOf("-map", "-cookcultures", "-unversionedcookedcontent"),
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should create options from the given runner parameters`(case: TestCase) {
        val actual = CookOptions.from(case.runnerParameters)

        assertEquals(case.expectedOptions, actual)
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should generate a correct list of arguments`(case: TestCase) {
        val arguments = case.expectedOptions.arguments

        assertTrue(arguments.containsAll(case.shouldContainItems))
        assertTrue(case.shouldNotContainItems.all { !arguments.contains(it) })
    }
}
