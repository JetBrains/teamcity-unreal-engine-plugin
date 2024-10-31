package com.jetbrains.teamcity.plugins.unrealengine.common

import arrow.core.raise.Raise
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

fun Raise<Error>.raise(message: String): Nothing = raise(GenericError(message))

fun Raise<Error>.raise(
    message: String,
    exception: Throwable?,
): Nothing = raise(GenericError(message, exception))

fun Raise<Error>.ensure(
    condition: Boolean,
    message: String,
) = ensure(condition) { raise(message) }

fun <T : Any> Raise<Error>.ensureNotNull(
    value: T?,
    message: String,
): T = ensureNotNull(value) { raise(message) }
