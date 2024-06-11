package com.jetbrains.teamcity.plugins.unrealengine.common

data class ValidationError(
    val propertyName: String,
    val message: String,
)
