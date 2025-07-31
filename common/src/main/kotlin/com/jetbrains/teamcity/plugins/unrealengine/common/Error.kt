package com.jetbrains.teamcity.plugins.unrealengine.common

import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull

/**
 * This is a reasonable error type (along with [GenericError]) to use throughout the codebase
 */
interface Error

data class GenericError(
    val message: String,
    val exception: Throwable? = null,
) : Error

@RaiseDSL
context(raise: Raise<GenericError>)
fun raise(message: String): Nothing = raise.raise(GenericError(message))

@RaiseDSL
context(raise: Raise<GenericError>)
fun raise(
    message: String,
    exception: Throwable?,
): Nothing = raise.raise(GenericError(message, exception))

@RaiseDSL
context(raise: Raise<GenericError>)
fun ensure(
    condition: Boolean,
    message: String,
) = raise.ensure(condition) { raise(message) }

@RaiseDSL
context(raise: Raise<GenericError>)
fun <T : Any> ensureNotNull(
    value: T?,
    message: String,
): T = raise.ensureNotNull(value) { raise(message) }
