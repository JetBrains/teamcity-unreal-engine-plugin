import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealTool
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolType
import com.jetbrains.teamcity.plugins.unrealengine.agent.commandlets.CommandletWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealEngineProcessListenerFactory
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletNameParameter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class CommandletWorkflowCreatorTests {
    private val environment = mockk<Environment>()
    private val processListenerFactory = mockk<UnrealEngineProcessListenerFactory>(relaxed = true)
    private val toolRegistry = mockk<UnrealToolRegistry>()
    private val context = createTestUnrealBuildContext()

    @BeforeEach
    fun init() {
        clearAllMocks()
        setupTestUnrealBuildContext(context)

        every { context.runnerParameters } returns
            mapOf(
                CommandletNameParameter.name to "ResavePackages",
            )

        with(toolRegistry) {
            coEvery {
                with(any<UnrealBuildContext>()) {
                    with(any<Raise<GenericError>>()) {
                        editor(any())
                    }
                }
            } returns UnrealTool("/foo/bar", UnrealToolType.Editor)
        }

        with(environment) {
            every { osType } returns OSType.MacOs
        }
    }

    @Test
    fun `contains a single command in the workflow`() =
        runTest {
            // arrange
            val creator = CommandletWorkflowCreator(toolRegistry, environment, processListenerFactory)

            // act
            val workflow = with(context) { either { creator.create() } }.getOrNull()

            // assert
            workflow shouldNotBe null
            workflow!!.commands shouldHaveSize 1
        }

}


