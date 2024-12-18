import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildParametersMap

class UnrealBuildContextStub(
    override val buildParameters: BuildParametersMap =
        mockk {
            every { environmentVariables } returns mapOf()
        },
    override val runnerParameters: Map<String, String> = emptyMap(),
    override val build: AgentRunningBuild = mockk<AgentRunningBuild>(),
    override val runnerId: String = "Unreal_Engine",
    override val runnerName: String = "Unreal Engine",
    override val workingDirectory: String = "foo",
    override val agentTempDirectory: String = "bar",
    val fileExistsStub: (String) -> Boolean = { true },
    val resolvePathStub: (String, List<String>) -> String = { root, parts ->
        listOf(root, *parts.toTypedArray()).joinToString(separator = "/")
    },
    val isAbsoluteStub: (String) -> Boolean = { true },
) : UnrealBuildContext {
    override fun resolvePath(
        root: String,
        vararg parts: String,
    ) = resolvePathStub(root, parts.toList())

    override fun fileExists(path: String) = fileExistsStub(path)

    override fun isAbsolute(path: String) = isAbsoluteStub(path)

    override fun createDirectory(
        root: String,
        vararg parts: String,
    ) = ""
}
