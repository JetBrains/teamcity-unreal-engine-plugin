package com.jetbrains.teamcity.plugins.unrealengine.common

fun String.escapeHTML(): String {
    val text = this
    if (text.isEmpty()) {
        return text
    }

    return buildString(length) {
        for (element in text) {
            when (element) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                else -> append(element)
            }
        }
    }
}
