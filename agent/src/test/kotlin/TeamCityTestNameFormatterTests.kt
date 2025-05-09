
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.TeamCityTestNameFormatter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TeamCityTestNameFormatterTests {
    data class TestNameFormatterTestCase(
        val testName: String,
        val testFullName: String,
        val formattedResult: String,
    )

    private fun `parses test started message`(): Collection<TestNameFormatterTestCase> =
        listOf(
            TestNameFormatterTestCase(
                testName = "Test with space in class name",
                testFullName = "PackageName.Class Name.Test with space in class name",
                formattedResult = "PackageName.Class_Name.Test with space in class name",
            ),
            TestNameFormatterTestCase(
                testName = "Test without space in class name",
                testFullName = "PackageName.ClassName.Test without space in class name",
                formattedResult = "PackageName.ClassName.Test without space in class name",
            ),
            TestNameFormatterTestCase(
                testName = "Test.name.with.dots",
                testFullName = "PackageName.ClassName.Test.name.with.dots",
                formattedResult = "PackageName.ClassName.Test_name_with_dots",
            ),
            TestNameFormatterTestCase(
                testName = "Test: name with semicolon",
                testFullName = "PackageName.ClassName.Test: name with semicolon",
                formattedResult = "PackageName.ClassName.Test name with semicolon",
            ),
            TestNameFormatterTestCase(
                testName = "Test name is not substring of test fullName",
                testFullName = "PackageName.ClassName.Other Test Name",
                formattedResult = "PackageName_ClassName_Other_Test_Name",
            ),
        )

    @ParameterizedTest
    @MethodSource("parses test started message")
    fun `parses test started message`(testCase: TestNameFormatterTestCase) {
        // act
        val result = TeamCityTestNameFormatter.format(testCase.testName, testCase.testFullName)

        // assert
        result shouldBe testCase.formattedResult
    }
}
