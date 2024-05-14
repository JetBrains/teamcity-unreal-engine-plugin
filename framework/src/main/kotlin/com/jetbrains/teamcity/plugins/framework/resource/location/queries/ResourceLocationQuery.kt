package com.jetbrains.teamcity.plugins.framework.resource.location.queries

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult

@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
open class ResourceLocationQuery<TContext, TResult> internal constructor (
    private val getValue: context(TContext, Raise<ResourceLocationResult.Error>) () -> TResult
) {
    context(TContext, Raise<ResourceLocationResult.Error>)
    @PublishedApi internal fun execute(): TResult = getValue(this@TContext, this@Raise)
}
