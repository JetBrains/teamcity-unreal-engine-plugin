package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOptionsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter

class BuildGraphComponent {
    val script = BuildGraphScriptPathParameter
    val target = BuildGraphTargetNodeParameter
    val options = BuildGraphOptionsParameter
    val mode = BuildGraphModeParameter
}
