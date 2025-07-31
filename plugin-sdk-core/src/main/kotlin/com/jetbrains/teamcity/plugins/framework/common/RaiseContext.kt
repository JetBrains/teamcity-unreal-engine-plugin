@file:OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)

package com.jetbrains.teamcity.plugins.framework.common

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.RaiseDSL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import arrow.core.raise.ensure as ensureExt
import arrow.core.raise.ensureNotNull as ensureNotNullExt
import arrow.core.raise.withError as withErrorExt
import arrow.core.raise.zipOrAccumulate as zipOrAccumulateExt

/*
 * This file contains temporary "bridge" functions until they are released in Arrow.
 * See https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md#simulating-receivers
 */

context(raise: Raise<Error>) @RaiseDSL
fun <Error> raise(e: Error): Nothing =
    raise.raise(e)

context(raise: Raise<Error>) @RaiseDSL
inline fun <Error> ensure(condition: Boolean, otherwise: () -> Error) {
    contract { returns() implies condition }
    raise.ensureExt(condition, otherwise)
}

context(raise: Raise<Error>) @RaiseDSL
inline fun <Error, B : Any> ensureNotNull(value: B?, otherwise: () -> Error): B {
    contract { returns() implies (value != null) }
    return raise.ensureNotNullExt(value, otherwise)
}

context(raise: Raise<Error>) @RaiseDSL
inline fun <Error, OtherError, A> withError(
    transform: (OtherError) -> Error,
    @BuilderInference block: context(Raise<OtherError>) () -> A
): A = raise.withErrorExt(transform, block)

context(raise: Raise<NonEmptyList<Error>>)
@RaiseDSL
inline fun <Error, A, B, D> zipOrAccumulate(
    @BuilderInference action1: context(RaiseAccumulate<Error>) () -> A,
    @BuilderInference action2: context(RaiseAccumulate<Error>) () -> B,
    block: (A, B) -> D
): D = raise.zipOrAccumulateExt(action1, action2, block)

context(raise: Raise<NonEmptyList<Error>>)
@RaiseDSL
inline fun <Error, A, B, C, D> zipOrAccumulate(
    @BuilderInference action1: context(RaiseAccumulate<Error>) () -> A,
    @BuilderInference action2: context(RaiseAccumulate<Error>) () -> B,
    @BuilderInference action3: context(RaiseAccumulate<Error>) () -> C,
    block: (A, B, C) -> D
): D = raise.zipOrAccumulateExt(action1, action2, action3, block)

context(raise: Raise<NonEmptyList<Error>>)
@RaiseDSL
inline fun <Error, A, B, C, D, E> zipOrAccumulate(
    @BuilderInference action1: context(RaiseAccumulate<Error>) () -> A,
    @BuilderInference action2: context(RaiseAccumulate<Error>) () -> B,
    @BuilderInference action3: context(RaiseAccumulate<Error>) () -> C,
    @BuilderInference action4: context(RaiseAccumulate<Error>) () -> D,
    block: (A, B, C, D) -> E
): E = raise.zipOrAccumulateExt(action1, action2, action3, action4, block)
