package com.jetbrains.teamcity.plugins.unrealengine.common.parameters

object DefaultAdditionalParameters {
    // Output of the spawned process is expected to be encoded as UTF-8.
    private const val UTF8_OUTPUT = "-utf8output"

    // Undocumented parameter. Basically, it does a couple of things
    // * Detailed cook timing statistics to csv file
    // * Logs shader compiler errors
    // * No dialogs or crash reporter
    // * ...
    private const val BUILD_MACHINE = "-buildmachine"

    // Assumes no operator is present, always terminates without waiting for something
    private const val UNATTENDED = "-unattended"

    // We do not want to interact with Perforce during this build
    private const val NO_P4 = "-noP4"

    // no splash screen (initial graphical display that appears when you launch a game)
    private const val NO_SPLASH = "-nosplash"

    // all log lines go to stdout
    private const val STDOUT = "-stdout"

    // The -buildmachine option enables code signing by default.
    // However, it requires certain certificates to be installed on an agent. If they are missing, a build fails.
    // With this approach, our aim is to ensure a successful initial build.
    private const val NO_CODE_SIGN = "-NoCodeSign"

    fun get() =
        listOf(
            UTF8_OUTPUT,
            BUILD_MACHINE,
            UNATTENDED,
            NO_P4,
            NO_SPLASH,
            STDOUT,
            NO_CODE_SIGN,
        )
}

fun DefaultAdditionalParameters.allToString() = get().joinToString(separator = " ")
