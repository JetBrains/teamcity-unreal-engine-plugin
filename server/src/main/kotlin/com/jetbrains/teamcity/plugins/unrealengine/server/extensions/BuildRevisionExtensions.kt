package com.jetbrains.teamcity.plugins.unrealengine.server.extensions

import jetbrains.buildServer.serverSide.BuildRevision

fun BuildRevision.getPerforceChangelistNumber() = revision.splitToSequence("|").lastOrNull()?.toLongOrNull()
