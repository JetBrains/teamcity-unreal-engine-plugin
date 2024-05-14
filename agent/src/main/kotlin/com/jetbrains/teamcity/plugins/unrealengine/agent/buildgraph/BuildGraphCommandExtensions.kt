package com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph

import arrow.core.getOrElse
import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreationError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand

context(Raise<WorkflowCreationError>, UnrealBuildContext)
fun BuildGraphCommand.getArgumentsOrRaiseError(): List<String> =
    toArguments().getOrElse { raise(WorkflowCreationError.ExecutionPreparationError(it.message)) }
