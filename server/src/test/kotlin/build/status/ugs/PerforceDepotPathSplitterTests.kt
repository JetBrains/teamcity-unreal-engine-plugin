package build.status.ugs

import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.PerforceDepotPath
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.PerforceDepotPathSplitter
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.PerforceFilePath
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.PerforceStream
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PerforceDepotPathSplitterTests {
    private val splitter = PerforceDepotPathSplitter()

    @Test
    fun `splits well-formed depot path into stream and file path`() {
        // arrange
        val input = PerforceDepotPath("//depot/main/project/module")

        // act
        val (stream, filePath) = splitter.split(input)

        // assert
        stream shouldBe PerforceStream("//depot/main")
        filePath shouldBe PerforceFilePath("project/module")
    }

    @Test
    fun `splits depot path with exactly four segments`() {
        val input = PerforceDepotPath("//depot/stream")
        val (stream, filePath) = splitter.split(input)

        stream shouldBe PerforceStream("//depot/stream")
        filePath shouldBe PerforceFilePath("")
    }

    @Test
    fun `handles short depot path with fewer than four segments`() {
        val input = PerforceDepotPath("//depot")
        val (stream, filePath) = splitter.split(input)

        stream shouldBe PerforceStream("//depot")
        filePath shouldBe PerforceFilePath("")
    }

    @Test
    fun `trims trailing slashes before splitting`() {
        val input = PerforceDepotPath("//depot/stream/project/")
        val (stream, filePath) = splitter.split(input)

        stream shouldBe PerforceStream("//depot/stream")
        filePath shouldBe PerforceFilePath("project")
    }

    @Test
    fun `normalizes mixed casing to lowercase`() {
        val input = PerforceDepotPath("//Depot/Stream/Project/Module")
        val (stream, filePath) = splitter.split(input)

        stream shouldBe PerforceStream("//depot/stream")
        filePath shouldBe PerforceFilePath("project/module")
    }

    @Test
    fun `handles non-depot style path gracefully`() {
        val input = PerforceDepotPath("not/a/depot/path")
        val (stream, filePath) = splitter.split(input)

        stream shouldBe PerforceStream("not/a/depot/path")
        filePath shouldBe PerforceFilePath("")
    }
}
