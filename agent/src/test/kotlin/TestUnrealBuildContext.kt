
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildParametersMap

fun createTestUnrealBuildContext() =
    mockk<UnrealBuildContext>()
        .also { setupTestUnrealBuildContext(it) }

fun setupTestUnrealBuildContext(context: UnrealBuildContext) {
    every { context.runnerParameters } returns emptyMap()
    every { context.buildParameters } returns
        mockk<BuildParametersMap> {
            every { environmentVariables } returns emptyMap<String, String>()
        }
    every { context.build } returns mockk<AgentRunningBuild>()
    every { context.workingDirectory } returns "foo"
    every { context.agentTempDirectory } returns "bar"
    every { context.runnerId } returns "Unreal_Engine"
    every { context.runnerName } returns "Unreal Engine"
    every { context.fileExists(any()) } answers { true }
    every { context.isAbsolute(any()) } answers { true }
    every { context.resolvePath(any()) } answers { "" }
    every { context.resolveUserPath(any()) } answers { "${context.workingDirectory}/${firstArg<String>()}" }
}
