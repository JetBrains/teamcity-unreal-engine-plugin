package com.jetbrains.teamcity.plugins.unrealengine.server.extensions

import arrow.core.Either
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError

fun Logger.logError(
    error: GenericError,
    prefix: String = "",
) {
    val (message, exception) = error
    if (exception != null) {
        error("$prefix$message", exception)
    } else {
        error("$prefix$message")
    }
}

fun Logger.logResult(
    result: Either<Error, Unit>,
    context: String,
    successMessage: String? = null,
) {
    when (result) {
        is Either.Left -> {
            val errorMessagePrefix = "An error occurred in the context of \"$context\":"
            when (val error = result.value) {
                is GenericError -> {
                    if (error.exception != null) {
                        warn("$errorMessagePrefix ${error.message}", error.exception)
                    } else {
                        warn("$errorMessagePrefix ${error.message}")
                    }
                }
                else -> {
                    warn("An unexpected error occurred in the context of \"$context\"")
                }
            }
        }
        is Either.Right -> successMessage?.let { debug(it) }
    }
}
