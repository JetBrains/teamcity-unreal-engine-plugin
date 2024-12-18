package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletNameParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.EditorExecutableParameter

class RunCommandletComponent {
    val editorExecutable = EditorExecutableParameter
    val projectPath = CommandletProjectPathParameter
    val commandletName = CommandletNameParameter
    val commandletArguments = CommandletArgumentsParameter
}
