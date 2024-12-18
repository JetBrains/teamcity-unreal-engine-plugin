import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext

class CommandExecutionContextStub(
    override val workingDirectory: String = "foo",
    override val agentTempDirectory: String = "bar",
    val fileExistsStub: (String) -> Boolean = { true },
    val resolvePathStub: (String, List<String>) -> String = { root, parts ->
        val sanitizedRoot = root.trimEnd('/')
        val sanitizedParts =
            parts
                .flatMap { it.split("/") }
                .filter { it != "." }

        sequenceOf(sanitizedRoot, *sanitizedParts.toTypedArray())
            .filter { it.isNotEmpty() }
            .joinToString("/")
    },
    val isAbsoluteStub: (String) -> Boolean = { true },
) : CommandExecutionContext {
    override fun resolvePath(
        root: String,
        vararg parts: String,
    ): String = resolvePathStub(root, parts.toList())

    override fun fileExists(path: String) = fileExistsStub(path)

    override fun isAbsolute(path: String) = isAbsoluteStub(path)

    override fun createDirectory(
        root: String,
        vararg parts: String,
    ) = ""
}
