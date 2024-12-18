package com.jetbrains.teamcity.plugins.unrealengine.common

import java.time.Clock

class ClockFactory {
    companion object {
        @JvmStatic
        fun systemClock(): Clock = Clock.systemUTC()
    }
}
