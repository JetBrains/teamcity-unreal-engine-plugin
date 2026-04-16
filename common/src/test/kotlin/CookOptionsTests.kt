
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookCulture
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.AdditionalCookerOptionsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookAllParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookCulturesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookMap
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookMapsOnlyParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookOptions
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookPartialGCParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ExcludeEditorContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.FastCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.GenerateChunksParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.IgnoreCookErrorsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.IterativeCookingParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.MapsToCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.SkipCookingEditorContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnversionedCookedContentParameter
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CookOptionsTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: CookOptions,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    private fun `test cases`() =
        listOf(
            TestCase(
                runnerParameters =
                    mapOf(
                        MapsToCookParameter.name to "Map1+Map2+Map3",
                        CookCulturesParameter.name to "Culture1+Culture2",
                        UnversionedCookedContentParameter.name to true.toString(),
                        GenerateChunksParameter.name to true.toString(),
                        IterativeCookingParameter.name to true.toString(),
                        CookAllParameter.name to true.toString(),
                        CookMapsOnlyParameter.name to true.toString(),
                        CookPartialGCParameter.name to true.toString(),
                        FastCookParameter.name to true.toString(),
                        IgnoreCookErrorsParameter.name to true.toString(),
                        SkipCookingEditorContentParameter.name to true.toString(),
                        ExcludeEditorContentParameter.name to true.toString(),
                        AdditionalCookerOptionsParameter.name to
                            "-cookincremental -tracefile=%teamcity.build.checkoutDir%/Saved/CookInsights.utrace",
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
                        generateChunks = true,
                        iterativeCooking = true,
                        cookAll = true,
                        cookMapsOnly = true,
                        cookPartialGC = true,
                        fastCook = true,
                        ignoreCookErrors = true,
                        skipCookingEditorContent = true,
                        excludeEditorContent = true,
                        additionalCookerOptions =
                            "-cookincremental -tracefile=%teamcity.build.checkoutDir%/Saved/CookInsights.utrace",
                    ),
                shouldContainItems =
                    listOf(
                        "-cook",
                        "-map=Map1+Map2+Map3",
                        "-cookcultures=Culture1+Culture2",
                        "-unversionedcookedcontent",
                        "-generatechunks",
                        "-iterativecooking",
                        "-cookall",
                        "-cookmapsonly",
                        "-CookPartialGC",
                        "-FastCook",
                        "-IgnoreCookErrors",
                        "-SkipCookingEditorContent",
                        "-ExcludeEditorContent",
                        "-AdditionalCookerOptions=\"-cookincremental -tracefile=%teamcity.build.checkoutDir%/Saved/CookInsights.utrace\"",
                    ),
                shouldNotContainItems = emptyList(),
            ),
            TestCase(
                runnerParameters = mapOf(),
                expectedOptions = CookOptions(),
                shouldContainItems = listOf("-cook"),
                shouldNotContainItems =
                    listOf(
                        "-map",
                        "-cookcultures",
                        "-unversionedcookedcontent",
                        "-generatechunks",
                        "-iterativecooking",
                        "-cookall",
                        "-cookmapsonly",
                        "-CookPartialGC",
                        "-FastCook",
                        "-IgnoreCookErrors",
                        "-SkipCookingEditorContent",
                        "-ExcludeEditorContent",
                        "-AdditionalCookerOptions",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("test cases")
    fun `creates options from the given runner parameters`(case: TestCase) {
        // act
        val options = CookOptions.from(case.runnerParameters)

        // assert
        options shouldBe case.expectedOptions
    }

    @ParameterizedTest
    @MethodSource("test cases")
    fun `generates a correct list of arguments`(case: TestCase) {
        // act
        val arguments = case.expectedOptions.arguments

        // assert
        arguments shouldContainAll case.shouldContainItems
        if (case.shouldNotContainItems.isNotEmpty()) {
            arguments shouldNotContainAnyOf case.shouldNotContainItems
        }
    }
}
