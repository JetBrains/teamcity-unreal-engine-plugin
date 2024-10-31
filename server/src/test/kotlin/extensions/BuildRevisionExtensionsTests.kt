package extensions

import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getPerforceChangelistNumber
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.serverSide.BuildRevision
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildRevisionExtensionsTests {
    private val buildRevision = mockk<BuildRevision>()

    data class PerforceChangeNumberTestCase(
        val revision: String,
        val changeNumber: Long? = null,
    )

    private fun `correctly determines Perforce changelist number`(): Collection<PerforceChangeNumberTestCase> =
        listOf(
            PerforceChangeNumberTestCase(
                "123",
                123,
            ),
            PerforceChangeNumberTestCase(
                "foo|123",
                123,
            ),
            PerforceChangeNumberTestCase(
                "foo|bar|123",
                123,
            ),
        )

    @ParameterizedTest
    @MethodSource("correctly determines Perforce changelist number")
    fun `correctly determines Perforce changelist number`(case: PerforceChangeNumberTestCase) {
        // arrange
        every { buildRevision.revision } returns case.revision

        // act
        val result = buildRevision.getPerforceChangelistNumber()

        // assert
        result shouldNotBe beNull()
        result shouldBe case.changeNumber
    }

    private fun `returns null when cannot determine Perforce changelist number`(): Collection<PerforceChangeNumberTestCase> =
        listOf(
            PerforceChangeNumberTestCase(
                "foo",
            ),
            PerforceChangeNumberTestCase(
                "foo|bar",
            ),
        )

    @ParameterizedTest
    @MethodSource("returns null when cannot determine Perforce changelist number")
    fun `returns null when cannot determine Perforce changelist number`(case: PerforceChangeNumberTestCase) {
        // arrange
        every { buildRevision.revision } returns "foo"

        // act
        val result = buildRevision.getPerforceChangelistNumber()

        // result
        result shouldBe null
    }
}
