package resource.location

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.resource.location.parseIni
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ParseIniTests {
    private val content = """
    default = ok

    [section1]
    var1 = foo
    var2 = bar

    [emptySection]
    """.trimIndent()

    @Test
    fun `should raise an error when the requested section does not exist`() {
        val result = either { StringReader(content).parseIni("unknown") }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should return an empty list when the requested section has not entries`() {
        val result = either { StringReader(content).parseIni("emptySection") }

        val propertyList = result.getOrNull()
        assertNotNull(propertyList)
        assertEquals(0, propertyList.size)
    }

    @Test
    fun `should return entries under specified section`() {
        val result = either { StringReader(content).parseIni("section1") }

        val propertyList = result.getOrNull()
        assertNotNull(propertyList)
        assertEquals(2, propertyList.size)
        assertNotNull(propertyList.find { it.key == "var1" && it.value == "foo" })
        assertNotNull(propertyList.find { it.key == "var2" && it.value == "bar" })
    }
}
