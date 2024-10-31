package build.status.ugs

import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerSettings
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import jetbrains.buildServer.web.functions.InternalProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UgsMetadataServerSettingsTests {
    init {
        mockkStatic(InternalProperties::getProperty)
        mockkStatic(InternalProperties::getInteger)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()
        val defaultValueSlot = slot<String>()
        every { InternalProperties.getProperty(any(), capture(defaultValueSlot)) } answers { defaultValueSlot.captured }

        val defaultValueIntSlot = slot<Int>()
        every { InternalProperties.getInteger(any(), capture(defaultValueIntSlot)) } answers { defaultValueIntSlot.captured }
    }

    @Test
    fun `dynamically returns retry count`() {
        // arrange
        val first = 5
        val second = 10
        every { InternalProperties.getInteger(any(), any()) } returns first andThen second

        // act
        val settings = UgsMetadataServerSettings()
        val firstRetryCount = settings.retryCount
        val secondRetryCount = settings.retryCount

        // assert
        firstRetryCount shouldBe first
        secondRetryCount shouldBe second
    }

    @Test
    fun `dynamically returns request timeout`() {
        // arrange
        val first = 5.toDuration(DurationUnit.SECONDS)
        val second = 10.toDuration(DurationUnit.SECONDS)
        every { InternalProperties.getProperty(any(), any()) } returns first.toIsoString() andThen second.toIsoString()

        // act
        val settings = UgsMetadataServerSettings()
        val firstRequestTimeout = settings.requestTimeout
        val secondRequestTimeout = settings.requestTimeout

        // assert
        firstRequestTimeout shouldBe first
        secondRequestTimeout shouldBe second
    }

    @Test
    fun `use default values when nothing specified`() {
        // act
        val settings = UgsMetadataServerSettings()

        // assert
        settings.retryCount shouldBe 3
        settings.requestTimeout shouldBe 5.toDuration(DurationUnit.SECONDS)
    }
}
