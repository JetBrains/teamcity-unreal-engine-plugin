package buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphVirtualBuildCreator
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.create
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jetbrains.buildServer.BuildTypeDescriptor.CheckoutType
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.virtualConfiguration.generator.VirtualBuildTypeSettings
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.function.BiFunction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuildGraphVirtualBuildCreatorTests {
    private val originalBuild = mockk<BuildPromotionEx>(relaxed = true)
    private val buildGeneratorFactory = mockk<VirtualPromotionGeneratorFactory>()
    private val buildTypeConfigurationSlot = slot<BiFunction<SBuildType, String, Boolean>>()
    private val virtualBuildTypeSettingsSlot = slot<VirtualBuildTypeSettings>()

    @BeforeEach
    fun init() {
        clearAllMocks()

        with(originalBuild) {
            withCheckoutSettings("foo", CheckoutType.AUTO)
        }

        with(buildGeneratorFactory) {
            every { create(originalBuild) } returns
                mockk {
                    every {
                        getOrCreate(
                            capture(virtualBuildTypeSettingsSlot),
                            capture(buildTypeConfigurationSlot),
                        )
                    } returns mockk<BuildPromotionEx>(relaxed = true)
                }
        }
    }

    data class CheckoutSettingsSyncTestCase(
        val checkoutDirectory: String,
        val checkoutType: CheckoutType,
    )

    private fun `syncs checkout settings with the original build`() =
        listOf(
            CheckoutSettingsSyncTestCase("foo", CheckoutType.MANUAL),
            CheckoutSettingsSyncTestCase("bar", CheckoutType.ON_SERVER),
            CheckoutSettingsSyncTestCase("baz", CheckoutType.AUTO),
        )

    @ParameterizedTest
    @MethodSource("syncs checkout settings with the original build")
    fun `syncs checkout settings with the original build`(testCase: CheckoutSettingsSyncTestCase) {
        // arrange
        originalBuild.withCheckoutSettings(testCase.checkoutDirectory, testCase.checkoutType)

        val buildCreator = BuildGraphVirtualBuildCreator(buildGeneratorFactory)

        // act
        with(buildCreator.inContextOf(originalBuild)) { buildCreator.create("foo") {} }

        // assert
        assertFalse(buildTypeConfigurationSlot.isNull)
        val checkoutDirectorySlot = slot<String>()
        val checkoutTypeSlot = slot<CheckoutType>()
        val buildTypeMock =
            mockk<SBuildType>(relaxed = true) {
                every { checkoutDirectory = capture(checkoutDirectorySlot) } answers {}
                every { checkoutType = capture(checkoutTypeSlot) } answers {}
            }
        buildTypeConfigurationSlot.captured.apply(buildTypeMock, "ignored")
        assertEquals(testCase.checkoutDirectory, checkoutDirectorySlot.captured)
        assertEquals(testCase.checkoutType, checkoutTypeSlot.captured)
    }

    @Test
    fun `propagates configuration parameters from the original build`() {
        // arrange
        val buildCreator = BuildGraphVirtualBuildCreator(buildGeneratorFactory)
        val originalBuildParameters =
            mapOf(
                "foo.parameter.key" to "foo.parameter.value",
                "bar.parameter.key" to "bar.parameter.value",
            )
        every { originalBuild.parameters } returns originalBuildParameters

        // act
        with(buildCreator.inContextOf(originalBuild)) { buildCreator.create("foo") {} }

        // assert
        assertFalse(virtualBuildTypeSettingsSlot.isNull)
        val virtualBuildTypeParameters = virtualBuildTypeSettingsSlot.captured.parameters?.associateBy { it.name }
        assertNotNull(virtualBuildTypeParameters)
        assertTrue {
            originalBuildParameters.all {
                virtualBuildTypeParameters[it.key]?.value == it.value
            }
        }
    }

    private fun BuildPromotionEx.withCheckoutSettings(
        directory: String,
        type: CheckoutType,
    ) {
        every { checkoutDirectory } returns directory
        every { buildSettings } returns
            mockk {
                every { checkoutType } returns type
            }
    }
}
