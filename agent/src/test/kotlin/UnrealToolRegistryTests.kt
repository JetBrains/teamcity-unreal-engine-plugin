
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProvider
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.EditorExecutableParameter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UnrealToolRegistryTests {
    private val environment = mockk<Environment>()
    private val engineProvider = mockk<UnrealEngineProvider>()
    private val commandExecutionContext = mockk<CommandExecutionContext>()

    private fun resolvePath(
        root: String,
        parts: List<String>,
    ): String {
        val sanitizedRoot = root.trimEnd('/')
        val sanitizedParts =
            parts
                .flatMap { it.split("/") }
                .filter { it != "." }

        return listOf(sanitizedRoot, *sanitizedParts.toTypedArray()).joinToString("/")
    }

    private fun isAbsolute(path: String) = path.startsWith("/") || path.startsWith("C")

    @BeforeEach
    fun init() {
        clearAllMocks()
        with(commandExecutionContext) {
            every { isAbsolute(any()) } answers { this@UnrealToolRegistryTests.isAbsolute(firstArg()) }
            every { resolvePath(any(), *anyVararg()) } answers {
                val root = firstArg<String>()
                val partsArray = args[1] as Array<*>
                val partsList = partsArray.filterIsInstance<String>()
                this@UnrealToolRegistryTests.resolvePath(root, partsList)
            }
        }
    }

    data class EditorPathTestCase(
        val os: OSType,
        val engine: UnrealEngine,
        val specifiedEditor: String?,
        val expectedPath: String,
    )

    private fun `correctly determines path to the Editor`(): Collection<EditorPathTestCase> =
        buildList {
            val defaultPaths =
                listOf(
                    EditorPathTestCase(
                        os = OSType.Windows,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("C:\\EngineRoot"),
                                UnrealEngineVersion(4, 27, 0),
                            ),
                        specifiedEditor = null,
                        expectedPath =
                            resolvePath(
                                "C:\\EngineRoot",
                                listOf("Engine", "Binaries", "Win64", "UE4Editor.exe"),
                            ),
                    ),
                    EditorPathTestCase(
                        os = OSType.Linux,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("/EngineRoot"),
                                UnrealEngineVersion(4, 27, 0),
                            ),
                        specifiedEditor = null,
                        expectedPath = resolvePath("/EngineRoot", listOf("Engine", "Binaries", "Linux", "UE4Editor")),
                    ),
                    EditorPathTestCase(
                        os = OSType.MacOs,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("/EngineRoot"),
                                UnrealEngineVersion(4, 27, 0),
                            ),
                        specifiedEditor = null,
                        expectedPath = resolvePath("/EngineRoot", listOf("Engine", "Binaries", "Mac", "UE4Editor")),
                    ),
                    EditorPathTestCase(
                        os = OSType.Windows,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("C:\\EngineRoot"),
                                UnrealEngineVersion(5, 1, 0),
                            ),
                        specifiedEditor = null,
                        expectedPath =
                            resolvePath(
                                "C:\\EngineRoot",
                                listOf("Engine", "Binaries", "Win64", "UnrealEditor.exe"),
                            ),
                    ),
                    EditorPathTestCase(
                        os = OSType.Linux,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("/EngineRoot"),
                                UnrealEngineVersion(5, 3, 0),
                            ),
                        specifiedEditor = null,
                        expectedPath = resolvePath("/EngineRoot", listOf("Engine", "Binaries", "Linux", "UnrealEditor")),
                    ),
                    EditorPathTestCase(
                        os = OSType.MacOs,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("/EngineRoot"),
                                UnrealEngineVersion(5, 5, 0),
                            ),
                        specifiedEditor = null,
                        expectedPath = resolvePath("/EngineRoot", listOf("Engine", "Binaries", "Mac", "UnrealEditor")),
                    ),
                )
            addAll(defaultPaths)

            val absolutePath =
                EditorPathTestCase(
                    os = OSType.Linux,
                    engine =
                        UnrealEngine(
                            UnrealEngineRootPath("/EngineRoot"),
                            UnrealEngineVersion(5, 5, 0),
                        ),
                    specifiedEditor = "/another-root/path/to/editor",
                    expectedPath = "/another-root/path/to/editor",
                )
            add(absolutePath)

            val executableNames =
                listOf(
                    EditorPathTestCase(
                        os = OSType.Windows,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("C:\\EngineRoot"),
                                UnrealEngineVersion(5, 5, 0),
                            ),
                        specifiedEditor = "UnrealEditor-Cmd.exe",
                        expectedPath =
                            resolvePath(
                                "C:\\EngineRoot",
                                listOf("Engine", "Binaries", "Win64", "UnrealEditor-Cmd.exe"),
                            ),
                    ),
                    EditorPathTestCase(
                        os = OSType.Windows,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("C:\\EngineRoot"),
                                UnrealEngineVersion(5, 5, 0),
                            ),
                        specifiedEditor = "UnrealEditor-Cmd",
                        expectedPath =
                            resolvePath(
                                "C:\\EngineRoot",
                                listOf("Engine", "Binaries", "Win64", "UnrealEditor-Cmd.exe"),
                            ),
                    ),
                    EditorPathTestCase(
                        os = OSType.Linux,
                        engine =
                            UnrealEngine(
                                UnrealEngineRootPath("/EngineRoot"),
                                UnrealEngineVersion(5, 5, 0),
                            ),
                        specifiedEditor = "UnrealEditor-Cmd.exe",
                        expectedPath =
                            resolvePath(
                                "/EngineRoot",
                                listOf("Engine", "Binaries", "Linux", "UnrealEditor-Cmd"),
                            ),
                    ),
                )
            addAll(executableNames)

            val relativePath =
                EditorPathTestCase(
                    os = OSType.Windows,
                    engine =
                        UnrealEngine(
                            UnrealEngineRootPath("C:\\EngineRoot"),
                            UnrealEngineVersion(5, 5, 0),
                        ),
                    specifiedEditor = "./Folder/CustomEditor.exe",
                    expectedPath = resolvePath("C:\\EngineRoot", listOf("Folder", "CustomEditor.exe")),
                )
            add(relativePath)
        }

    @ParameterizedTest
    @MethodSource("correctly determines path to the Editor")
    fun `correctly determines path to the Editor`(testCase: EditorPathTestCase) {
        // arrange
        every { environment.osType } returns testCase.os

        engineProvider providesEngine testCase.engine

        // act
        val result =
            getEditor(
                mapOf(
                    EditorExecutableParameter.name to testCase.specifiedEditor.orEmpty(),
                ),
            )

        // assert
        result shouldNotBe null
        result!!.executablePath shouldBe testCase.expectedPath
    }

    data class AutomationToolPathTestCase(
        val osType: OSType,
        val engine: UnrealEngine,
        val expectedPath: String,
    )

    private fun `correctly determines path to the UAT launch script`(): Collection<AutomationToolPathTestCase> =
        listOf(
            AutomationToolPathTestCase(
                osType = OSType.Windows,
                engine =
                    UnrealEngine(
                        UnrealEngineRootPath("C:\\EngineRoot"),
                        UnrealEngineVersion(5, 0, 0),
                    ),
                expectedPath = resolvePath("C:\\EngineRoot", listOf("Engine", "Build", "BatchFiles", "RunUAT.bat")),
            ),
            AutomationToolPathTestCase(
                osType = OSType.MacOs,
                engine =
                    UnrealEngine(
                        UnrealEngineRootPath("/EngineRoot"),
                        UnrealEngineVersion(5, 0, 0),
                    ),
                expectedPath = resolvePath("/EngineRoot", listOf("Engine", "Build", "BatchFiles", "RunUAT.sh")),
            ),
            AutomationToolPathTestCase(
                osType = OSType.Linux,
                engine =
                    UnrealEngine(
                        UnrealEngineRootPath("/EngineRoot"),
                        UnrealEngineVersion(5, 0, 0),
                    ),
                expectedPath = resolvePath("/EngineRoot", listOf("Engine", "Build", "BatchFiles", "RunUAT.sh")),
            ),
        )

    @ParameterizedTest
    @MethodSource("correctly determines path to the UAT launch script")
    fun `correctly determines path to the UAT launch script`(testCase: AutomationToolPathTestCase) {
        // arrange
        every { environment.osType } returns testCase.osType

        engineProvider providesEngine testCase.engine

        // act
        val result = getAutomationTool()

        // assert
        result shouldNotBe null
        result!!.executablePath shouldBe testCase.expectedPath
    }

    private infix fun UnrealEngineProvider.providesEngine(engine: UnrealEngine) {
        coEvery {
            with(any<CommandExecutionContext>()) {
                with(any<Raise<GenericError>>()) {
                    engineProvider.findEngine(any())
                }
            }
        } returns engine
    }

    private fun getEditor(runnerParameters: Map<String, String> = emptyMap()) =
        runBlocking {
            either {
                with(commandExecutionContext) {
                    UnrealToolRegistry(engineProvider, environment).editor(runnerParameters)
                }
            }.getOrNull()
        }

    private fun getAutomationTool(runnerParameters: Map<String, String> = emptyMap()) =
        runBlocking {
            either {
                with(commandExecutionContext) {
                    UnrealToolRegistry(engineProvider, environment).automationTool(runnerParameters)
                }
            }
        }.getOrNull()
}
