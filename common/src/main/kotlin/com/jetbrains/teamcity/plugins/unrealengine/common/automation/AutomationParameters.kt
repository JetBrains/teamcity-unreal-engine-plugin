package com.jetbrains.teamcity.plugins.unrealengine.common.automation

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.ValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.ExecCommand.*
import com.jetbrains.teamcity.plugins.unrealengine.common.enumValueOfOrNull
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.RunnerParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectOption
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealProjectPathParameter

object NullRHIParameter : CheckboxParameter {
    override val name = "automation-null-rhi"
    override val displayName = "nullRHI"
    override val defaultValue = true.toString()
    override val description = "Use this flag to disable rendering."
    override val advanced = true
}

object AutomationExecCommandParameter : SelectParameter() {
    val all = SelectOption("run-all", "RunAll")
    val filter = SelectOption("run-filter", "RunFilter")
    val list = SelectOption("run-tests", "RunTests")

    override val name = "automation-exec-command"
    override val displayName = "Exec command"
    override val description = "Adds the -execcmds flag with 'Automation <selected mode>' value."
    override val defaultValue = ""

    override val options: List<SelectOption>
        get() = listOf(all, filter, list)

    context(Raise<ValidationError>)
    fun parse(properties: Map<String, String>): ExecCommand {
        val type = properties[name] ?: raise(ValidationError(name, "Automation exec command is missing."))

        return when (type) {
            all.name -> RunAll

            filter.name ->
                RunFilter(
                    AutomationFilterParameter.parse(properties)
                        ?: raise(ValidationError(AutomationFilterParameter.name, "Empty test filter.")),
                )

            list.name -> {
                val tests = AutomationTestsParameter.parse(properties)

                RunTests(
                    if (tests.any()) {
                        tests
                    } else {
                        raise(ValidationError(AutomationTestsParameter.name, "Empty list of test names."))
                    },
                )
            }

            else -> raise(ValidationError(name, "Unknown automation test run mode."))
        }
    }
}

object AutomationFilterParameter : SelectParameter() {
    override val name = "automation-filter"
    override val displayName = "Filter"
    override val defaultValue = RunFilterType.Product.name
    override val description = "Run only those tests that are tagged with this filter."
    override val options: List<SelectOption>
        get() = RunFilterType.entries.map { SelectOption(it.name) }

    fun parse(properties: Map<String, String>): RunFilterType? = properties[name]?.let { enumValueOfOrNull<RunFilterType>(it) }
}

object AutomationTestsParameter : RunnerParameter {
    override val name = "automation-tests"
    override val displayName = "Tests"
    override val defaultValue = ""
    val description =
        """
        A newline-delimited list of test names. Supports both full and partial hierarchical test names.
        """.trimIndent()

    fun parse(properties: Map<String, String>): List<UnrealAutomationTest> {
        val testsString = properties[name]
        if (testsString.isNullOrEmpty()) {
            return emptyList()
        }

        return testsString
            .split("\r\n", "\r", "\n")
            .filter { it.isNotEmpty() }
            .map { UnrealAutomationTest(it) }
    }
}

val AutomationProjectPathParameter = UnrealProjectPathParameter("automation-project-path")
