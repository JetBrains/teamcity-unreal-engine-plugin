package buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphParentTagSyncListener
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildPromotionManager
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SRunningBuild
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class BuildGraphParentTagSyncListenerTests {
    private val promotionManager = mockk<BuildPromotionManager>()
    private val childBuild = mockk<SRunningBuild>()
    private val childPromotion = mockk<BuildPromotionEx>()
    private val parentPromotion = mockk<BuildPromotionEx>()
    private val parentBuild = mockk<SBuild>(relaxed = true)

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { childBuild.buildPromotion } returns childPromotion
        every { childBuild.buildId } returns 1001L

        every {
            childPromotion.getAttribute("teamcity.build.unreal-engine.build-graph.generated-by")
        } returns null

        every { promotionManager.findPromotionById(any()) } returns parentPromotion
        every { parentPromotion.associatedBuild } returns parentBuild

        every { parentBuild.buildId } returns 1000L
        every { parentBuild.tags } returns listOf("ParentTag")
        every { parentBuild.setTags(any()) } returns Unit
    }

    @Test
    fun `does nothing for non-generated builds`() {
        // arrange
        every { childBuild.tags } returns listOf("ChildTag")

        // act
        BuildGraphParentTagSyncListener(promotionManager).beforeBuildFinish(childBuild)

        // assert
        verify(exactly = 0) { promotionManager.findPromotionById(any()) }
        verify(exactly = 0) { parentBuild.setTags(any()) }
    }

    @Test
    fun `syncs tags to parent build for generated builds`() {
        // arrange
        every {
            childPromotion.getAttribute("teamcity.build.unreal-engine.build-graph.generated-by")
        } returns "1000"
        every { childBuild.tags } returns listOf("ChildTag", "ParentTag")

        // act
        BuildGraphParentTagSyncListener(promotionManager).beforeBuildFinish(childBuild)

        // assert
        verify { promotionManager.findPromotionById(1000L) }
        verify { parentBuild.setTags(listOf("ParentTag", "ChildTag")) }
    }

    @Test
    fun `does nothing when child has no tags`() {
        // arrange
        every {
            childPromotion.getAttribute("teamcity.build.unreal-engine.build-graph.generated-by")
        } returns "1000"
        every { childBuild.tags } returns emptyList()

        // act
        BuildGraphParentTagSyncListener(promotionManager).beforeBuildFinish(childBuild)

        // assert
        verify(exactly = 0) { parentBuild.setTags(any()) }
    }
}

