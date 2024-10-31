package com.jetbrains.teamcity.plugins.unrealengine.common

data class PropertyValidationError(
    val propertyName: String,
    val message: String,
) : Error
