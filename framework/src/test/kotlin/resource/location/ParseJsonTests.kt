package resource.location

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.resource.location.parseJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ParseJsonTests {
    @Serializable
    private data class Content(val foo: String)
    private val json = Json {ignoreUnknownKeys = true}

    @ParameterizedTest(name = "content: {0}")
    @ValueSource(strings = ["", "  ", "{}", "{ \"bar\": \"bar\" }"])
    fun `should raise an error when the json is invalid`(invalidContent: String) {
        val result = either { StringReader(invalidContent).parseJson<Content>(json) }

        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should return parsed object`() {
        val content = """
            {
                "foo": "bar",
                "extra": "baz"
            }
            """.trimIndent()

        val result = either { StringReader(content).parseJson<Content>(json) }

        val parsedObject = result.getOrNull()
        assertNotNull(parsedObject)
        assertEquals("bar", parsedObject.foo)
    }
}
