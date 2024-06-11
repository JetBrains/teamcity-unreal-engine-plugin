
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraph
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphNode
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphNodeGroup
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphParser
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BuildBuildGraphParserTests {
    companion object {
        @JvmStatic
        fun generateHappyPathTestCases(): Collection<TestCase> =
            buildList {
                add(
                    TestCase(
                        """
{
	"Groups": [
		{
			"Name": "AB Group",
			"Agent Types": [
				"AB Agent"
			],
			"Nodes": [
				{
					"Name": "A",
					"DependsOn": "",
					"RunEarly": false
				},
				{
					"Name": "B",
					"DependsOn": "A",
					"RunEarly": false
				}
			]
		}
	],
	"Badges": [
	],
	"Reports": [
	]
}
                        """.trimIndent(),
                        BuildGraph(
                            mapOf(
                                BuildGraphNodeGroup(
                                    "AB Group",
                                    listOf("AB Agent"),
                                    listOf(
                                        BuildGraphNode("A", listOf()),
                                        BuildGraphNode("B", listOf("A")),
                                    ),
                                ) to listOf(),
                            ),
                        ),
                    ),
                )

                val groupA =
                    BuildGraphNodeGroup(
                        "A Group",
                        listOf("A Agent 1", "A Agent 2"),
                        listOf(BuildGraphNode("A", listOf())),
                    )
                val groupB =
                    BuildGraphNodeGroup(
                        "B Group",
                        listOf("B Agent"),
                        listOf(BuildGraphNode("B", listOf("A"))),
                    )
                val groupC =
                    BuildGraphNodeGroup(
                        "C Group",
                        listOf("C Agent"),
                        listOf(BuildGraphNode("C", listOf("B"))),
                    )
                add(
                    TestCase(
                        """
{
	"Groups": [
		{
			"Name": "A Group",
			"Agent Types": [
				"A Agent 1",
                "A Agent 2"
			],
			"Nodes": [
				{
					"Name": "A",
					"DependsOn": "",
					"RunEarly": false
				}
			]
		},
        {
			"Name": "B Group",
			"Agent Types": [
				"B Agent"
			],
			"Nodes": [
				{
					"Name": "B",
					"DependsOn": "A",
					"RunEarly": false
				}
			]
		},
        {
			"Name": "C Group",
			"Agent Types": [
				"C Agent"
			],
			"Nodes": [
				{
					"Name": "C",
					"DependsOn": "B",
					"RunEarly": false
				}
			]
		}
	],
	"Badges": [
	],
	"Reports": [
	]
}
                        """.trimIndent(),
                        BuildGraph(
                            mapOf(
                                groupA to listOf(groupB),
                                groupB to listOf(groupC),
                                groupC to listOf(),
                            ),
                        ),
                    ),
                )

                val groupD =
                    BuildGraphNodeGroup(
                        "D Group",
                        listOf("D Agent"),
                        listOf(BuildGraphNode("D", listOf())),
                    )
                val groupE =
                    BuildGraphNodeGroup(
                        "E Group",
                        listOf("E Agent"),
                        listOf(BuildGraphNode("E", listOf("A", "D"))),
                    )
                add(
                    TestCase(
                        """
{
	"Groups": [
		{
			"Name": "A Group",
			"Agent Types": [
				"A Agent 1",
                "A Agent 2"
			],
			"Nodes": [
				{
					"Name": "A",
					"DependsOn": "",
					"RunEarly": false
				}
			]
		},
        {
			"Name": "D Group",
			"Agent Types": [
				"D Agent"
			],
			"Nodes": [
				{
					"Name": "D",
					"DependsOn": "",
					"RunEarly": false
				}
			]
		},
        {
			"Name": "E Group",
			"Agent Types": [
				"E Agent"
			],
			"Nodes": [
				{
					"Name": "E",
					"DependsOn": "A;D",
					"RunEarly": false
				}
			]
		}
	],
	"Badges": [
	],
	"Reports": [
	]
}
                        """.trimIndent(),
                        BuildGraph(
                            mapOf(
                                groupA to listOf(groupE),
                                groupD to listOf(groupE),
                                groupE to listOf(),
                            ),
                        ),
                    ),
                )
            }
    }

    data class TestCase(
        val text: String,
        val expectedBuildGraph: BuildGraph<BuildGraphNodeGroup>,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should correctly parse json and create in-memory graph representation`(case: TestCase) {
        // arrange
        val parser = BuildGraphParser()

        // act
        val result = either { parser.parse(case.text.byteInputStream()) }.getOrNull()

        // assert
        assertNotNull(result)
        assertEquals(case.expectedBuildGraph, result)
    }
}
