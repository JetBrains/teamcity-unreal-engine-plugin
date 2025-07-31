package com.jetbrains.teamcity.plugins.framework.resource.location.queries

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult

interface ResourceLocationContext

open class ResourceLocationQuery<TContext : ResourceLocationContext, TResult> internal constructor (
    private val getValue: context(TContext, Raise<ResourceLocationResult.Error>) () -> TResult
) {
    context(context: TContext, raise: Raise<ResourceLocationResult.Error>)
    @PublishedApi internal fun execute(): TResult = getValue(context, raise)
}
