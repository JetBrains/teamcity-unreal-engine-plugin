package resource.location

import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistryEntry
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistryParser
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistryValueType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WindowsRegistryParserTests {
    companion object {
        @JvmStatic
        private fun validValuesGenerator(): Collection<Arguments> {
            return listOf(
                WindowsRegistryEntry.Value("name", WindowsRegistryValueType.Str, "value"),
                WindowsRegistryEntry.Value("013aeb76-0330-47d6-9b8a-53f721395364", WindowsRegistryValueType.Str, "C:\\Program Files\\Epic Games\\UE_5.1-custom")
            ).map {
                Arguments {
                    arrayOf(
                        "    ${it.name}    ${it.type.id}    ${it.data}",
                        it
                    )
                }
            }
        }
    }

    private val parser = WindowsRegistryParser()

    @ParameterizedTest
    @ValueSource(strings = [
        "",
        "   ",
        "\t\t",
        "   HKEY_LOCAL_MACHINE\\Software\\Epic Games",
        "   abcd",
    ])
    fun `should return null when the key is invalid`(invalidValue: String) {
        val result = parser.tryParseKey(invalidValue)

        assertNull(result)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "HKEY_CURRENT_USER\\Software\\Epic Games\\Unreal Engine\\Builds",
        "HKEY_CURRENT_USER\\Software\\Epic Games\\Unreal Engine",
        "HKEY_CURRENT_USER\\Software\\Epic Games",
        "HKEY_CURRENT_USER\\Software",
    ])
    fun `should return parsed key`(validValue: String) {
        val result = parser.tryParseKey(validValue)

        assertNotNull(result)
        assertEquals(validValue, result.path)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "",
        "   ",
        "\t\t",
        "   HKEY_LOCAL_MACHINE\\Software\\Microsoft",
        "   abcd",
        "    abc    ",
        "    aa bb    REG_FOO    abc bb",
        "    aa bb REG_SZ abc bb",
    ])
    fun `should return null when the value is invalid`(invalidValue: String) {
        val result = parser.tryParseValue(invalidValue)

        assertNull(result)
    }

    @ParameterizedTest
    @MethodSource("validValuesGenerator")
    fun `should return parsed value`(text: String, expectedValue: WindowsRegistryEntry.Value) {
        val result = parser.tryParseValue(text)

        assertEquals(expectedValue, result)
    }
}
