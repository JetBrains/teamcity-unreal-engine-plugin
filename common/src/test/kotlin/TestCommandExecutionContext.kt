import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import io.mockk.every
import io.mockk.mockk

fun createTestCommandExecutionContext() =
    mockk<CommandExecutionContext>()
        .also { setupTestCommandExecutionContext(it) }

fun setupTestCommandExecutionContext(context: CommandExecutionContext) {
    every { context.workingDirectory } returns "foo"
    every { context.agentTempDirectory } returns "bar"
    every { context.fileExists(any()) } answers { true }
    every { context.isAbsolute(any()) } answers { true }
    every { context.resolvePath(any()) } answers { "" }
    every { context.resolveUserPath(any()) } answers { "${context.workingDirectory}/${firstArg<String>()}" }
}
