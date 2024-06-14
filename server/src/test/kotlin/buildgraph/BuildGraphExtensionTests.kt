package buildgraph
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraph
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.topologicalSort
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertTrue

class BuildGraphExtensionTests {
    data class DummyNode(
        val name: String,
    )

    companion object {
        private val node0 = DummyNode("node 0")
        private val node1 = DummyNode("node 1")
        private val node2 = DummyNode("node 2")
        private val node3 = DummyNode("node 3")

        private val firstBuildGraph =
            BuildGraph(
                mapOf(
                    node0 to listOf(node2, node3),
                    node1 to listOf(node2),
                    node2 to listOf(node3),
                    node3 to listOf(),
                ),
            )

        private val secondBuildGraph =
            BuildGraph(
                mapOf(
                    node3 to listOf(node0, node1, node2),
                    node2 to listOf(node0, node1),
                    node1 to listOf(node0),
                    node0 to listOf(),
                ),
            )

        @JvmStatic
        fun generateTopologicalSortTestCases(): Collection<TopologicalSortTestCase> =
            listOf(
                TopologicalSortTestCase(firstBuildGraph),
                TopologicalSortTestCase(secondBuildGraph),
            )
    }

    data class TopologicalSortTestCase(
        val buildGraph: BuildGraph<DummyNode>,
    )

    @ParameterizedTest
    @MethodSource("generateTopologicalSortTestCases")
    fun `should correctly sort the nodes of a graph`(case: TopologicalSortTestCase) {
        // arrange, act
        val result = case.buildGraph.topologicalSort()

        // assert
        result.forEachIndexed { nodePosition, node ->
            case.buildGraph.adjacencyList[node]!!.forEach { successor ->
                val successorPosition = result.indexOf(successor)
                assertTrue { successorPosition > nodePosition }
            }
        }
    }
}
