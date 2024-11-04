package agent

import com.jetbrains.teamcity.plugins.framework.agent.AgentParametersProvider
import com.jetbrains.teamcity.plugins.framework.agent.PrimaryAgentParametersSupplier
import com.jetbrains.teamcity.plugins.framework.agent.TeamCityParameter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class PrimaryAgentParametersSupplierTests {
    @Test
    fun `failure in one of the providers does not affect the final result`() {
        val fooParameter = TeamCityParameter("foo", "foo", TeamCityParameter.Type.ConfigurationParameter)
        val fooProvider = AgentParametersProvider { listOf(fooParameter) }
        val barParameter = TeamCityParameter("bar", "bar", TeamCityParameter.Type.ConfigurationParameter)
        val barProvider = AgentParametersProvider { listOf(barParameter) }
        val errorProneProvider = AgentParametersProvider { throw Exception("something went wrong") }
        val supplier = PrimaryAgentParametersSupplier(listOf(fooProvider, barProvider, errorProneProvider))

        val parameters = supplier.parameters

        assertNotNull(parameters)
        assertContains(parameters, fooParameter.key)
        assertContains(parameters, barParameter.key)
    }

    @Test
    fun `should perform parameters discovery only once`() {
        var counter = 0
        val countProvider = AgentParametersProvider {
            counter++
            listOf()
        }
        val supplier = PrimaryAgentParametersSupplier(listOf(countProvider))

        supplier.parameters
        supplier.environmentVariables
        supplier.systemProperties
        supplier.parameters

        assertEquals(1, counter)
    }

    private val configurationParameter = "foo"
    private val configurationParameterProvider = AgentParametersProvider {
        listOf(TeamCityParameter(configurationParameter, configurationParameter, TeamCityParameter.Type.ConfigurationParameter))
    }

    private val environmentVariable = "bar"
    private val environmentVariableProvider = AgentParametersProvider {
        listOf(TeamCityParameter(environmentVariable, environmentVariable, TeamCityParameter.Type.EnvironmentVariable))
    }

    private val systemProperty = "baz"
    private val systemPropertyProvider = AgentParametersProvider {
        listOf(TeamCityParameter(systemProperty, systemProperty, TeamCityParameter.Type.SystemProperty))
    }

    @Test
    fun `should only return configuration parameters when they are requested`() {
        val supplier = PrimaryAgentParametersSupplier(
            listOf(configurationParameterProvider, environmentVariableProvider, systemPropertyProvider),
        )

        val result = supplier.parameters

        assertEquals(1, result.size)
        assertContains(result, configurationParameter)
    }

    @Test
    fun `should only return environment variables when they are requested`() {
        val supplier = PrimaryAgentParametersSupplier(
            listOf(configurationParameterProvider, environmentVariableProvider, systemPropertyProvider),
        )

        val result = supplier.environmentVariables

        assertEquals(1, result.size)
        assertContains(result, environmentVariable)
    }

    @Test
    fun `should only return system properties when they are requested`() {
        val supplier = PrimaryAgentParametersSupplier(
            listOf(configurationParameterProvider, environmentVariableProvider, systemPropertyProvider),
        )

        val result = supplier.systemProperties

        assertEquals(1, result.size)
        assertContains(result, systemProperty)
    }
}
