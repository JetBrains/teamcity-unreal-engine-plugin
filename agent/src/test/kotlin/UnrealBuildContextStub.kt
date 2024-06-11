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
    val concatPathsStub: (String, String) -> String = { root, path -> "$root/$path" },
    val isAbsoluteStub: (String) -> Boolean = { true },
) : UnrealBuildContext {
    override fun concatPaths(
        root: String,
        path: String,
    ) = concatPathsStub(root, path)

    override fun fileExists(path: String) = fileExistsStub(path)

    override fun isAbsolute(path: String) = isAbsoluteStub(path)

    override fun createDirectory(
        root: String,
        vararg parts: String,
    ) = ""
}
