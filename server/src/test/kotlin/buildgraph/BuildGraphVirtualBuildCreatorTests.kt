package buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphVirtualBuildCreator
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.create
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jetbrains.buildServer.BuildTypeDescriptor.CheckoutType
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.virtualConfiguration.generator.VirtualBuildTypeSettings
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.function.BiFunction
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BuildGraphVirtualBuildCreatorTests {
    companion object {
        @JvmStatic
        fun checkoutSettingsSyncTestCases() =
            listOf(
                CheckoutSettingsSyncTestCase("foo", CheckoutType.MANUAL),
                CheckoutSettingsSyncTestCase("bar", CheckoutType.ON_SERVER),
                CheckoutSettingsSyncTestCase("baz", CheckoutType.AUTO),
            )
    }

    data class CheckoutSettingsSyncTestCase(
        val checkoutDirectory: String,
        val checkoutType: CheckoutType,
    )

    @ParameterizedTest
    @MethodSource("checkoutSettingsSyncTestCases")
    fun `should sync checkout settings with the original build`(testCase: CheckoutSettingsSyncTestCase) {
        // arrange
        val originalBuildMockk =
            mockk<BuildPromotionEx>(relaxed = true) {
                every { checkoutDirectory } returns testCase.checkoutDirectory
                every { buildSettings } returns
                    mockk {
                        every { checkoutType } returns testCase.checkoutType
                    }
            }

        val configurationSlot = slot<BiFunction<SBuildType, String, Boolean>>()

        val buildGeneratorFactoryMock =
            mockk<VirtualPromotionGeneratorFactory> {
                every { create(originalBuildMockk) } returns
                    mockk {
                        every {
                            getOrCreate(
                                any<VirtualBuildTypeSettings>(),
                                capture(configurationSlot),
                            )
                        } returns mockk<BuildPromotionEx>(relaxed = true)
                    }
            }

        val buildCreator = BuildGraphVirtualBuildCreator(buildGeneratorFactoryMock)

        // act
        with(buildCreator.inContextOf(originalBuildMockk)) { buildCreator.create("foo") {} }

        // assert
        assertFalse(configurationSlot.isNull)
        val checkoutDirectorySlot = slot<String>()
        val checkoutTypeSlot = slot<CheckoutType>()
        val buildTypeMock =
            mockk<SBuildType>(relaxed = true) {
                every { checkoutDirectory = capture(checkoutDirectorySlot) } answers {}
                every { checkoutType = capture(checkoutTypeSlot) } answers {}
            }
        configurationSlot.captured.apply(buildTypeMock, "ignored")
        assertEquals(testCase.checkoutDirectory, checkoutDirectorySlot.captured)
        assertEquals(testCase.checkoutType, checkoutTypeSlot.captured)
    }
}
