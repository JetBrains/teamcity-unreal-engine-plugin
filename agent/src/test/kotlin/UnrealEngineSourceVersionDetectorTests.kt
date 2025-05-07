
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocator
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineSourceVersionDetector
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class UnrealEngineSourceVersionDetectorTests {
    private val resourceLocatorMock = mockk<ResourceLocator>()
    private val locator = UnrealEngineSourceVersionDetector(resourceLocatorMock)
    private val context = createTestUnrealBuildContext()

    @BeforeEach
    fun init() {
        clearAllMocks()
        setupTestUnrealBuildContext(context)
    }

    @Test
    fun `detects version in 'Build version' file`() =
        runTest {
            // arrange
            val expectedVersion = UnrealEngineVersion(5, 3, 0)
            coEvery { resourceLocatorMock.locateResources<UnrealEngineVersion>(any()) } returns
                listOf(
                    ResourceLocationResult.Success(expectedVersion),
                )

            // act
            val result = act()

            // assert
            val version = result.getOrNull()
            version shouldBe expectedVersion
        }

    @Test
    fun `detects version in cpp version header file`() =
        runTest {
            // arrange
            val expectedVersion = UnrealEngineVersion(5, 3, 0)

            coEvery { resourceLocatorMock.locateResources<Any>(any()) } returnsMany
                listOf(
                    emptyList(),
                    listOf(
                        ResourceLocationResult.Success(
                            listOf(
                                "#define ENGINE_MAJOR_VERSION\t5",
                                "#define ENGINE_MINOR_VERSION\t3",
                                "#define ENGINE_PATCH_VERSION\t0",
                            ),
                        ),
                    ),
                )

            // act
            val result = act()

            // assert
            val version = result.getOrNull()
            version shouldBe expectedVersion
        }

    @Test
    fun `searches in cpp version header file only if search in 'Build version' fails`() =
        runTest {
            // arrange
            val expectedVersion = UnrealEngineVersion(5, 3, 0)
            coEvery { resourceLocatorMock.locateResources<UnrealEngineVersion>(any()) } returns
                listOf(
                    ResourceLocationResult.Success(expectedVersion),
                )

            // act
            act()

            // assert
            coVerify(exactly = 1) { resourceLocatorMock.locateResources<Any>(any()) }
        }

    @Test
    fun `raises error when unable to find anything`() =
        runTest {
            // arrange
            coEvery { resourceLocatorMock.locateResources<Any>(any()) } returnsMany
                listOf(
                    emptyList(),
                    listOf(
                        ResourceLocationResult.Success(
                            listOf(
                                "#define ENGINE_FOO_VERSION\t5",
                                "#define ENGINE_BAR_VERSION\t3",
                                "#define ENGINE_BAZ_VERSION\t0",
                            ),
                        ),
                    ),
                )

            // act
            val result = act()

            // assert
            val error = result.leftOrNull()
            error shouldNotBe null
        }

    private suspend fun act() =
        either {
            with(context) {
                locator.detect(UnrealEngineRootPath("foo"))
            }
        }
}
