import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext

class CommandExecutionContextStub(
    override val workingDirectory: String = "foo",
    override val agentTempDirectory: String = "bar",
    val fileExistsStub: (String) -> Boolean = { true },
    val concatPathsStub: (String, String) -> String = { root, path -> "$root/$path" },
    val isAbsoluteStub: (String) -> Boolean = { true },
) : CommandExecutionContext {
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
