package com.jetbrains.teamcity.plugins.unrealengine.server.extensions

import jetbrains.buildServer.serverSide.ServerResponsibility

fun ServerResponsibility.isMainNode() = canManageBuilds()
