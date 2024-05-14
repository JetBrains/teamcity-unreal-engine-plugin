package com.jetbrains.teamcity.plugins.unrealengine.server.discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import jetbrains.buildServer.util.browser.Element

interface UnrealProjectDiscoverer {
    fun discover(directory: Element): Collection<UnrealEngineProject>
}
