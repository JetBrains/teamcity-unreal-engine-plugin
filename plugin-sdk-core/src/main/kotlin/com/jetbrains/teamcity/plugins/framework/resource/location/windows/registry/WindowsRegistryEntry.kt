package com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry

sealed interface WindowsRegistryEntry {
    fun match(filter: WindowsRegistrySearchFilter): Boolean

    data class Value(val name: String, val type: WindowsRegistryValueType, val data: String) : WindowsRegistryEntry {
        override fun match(filter: WindowsRegistrySearchFilter): Boolean = filter.accept(this)
    }

    data class Key(val path: String) : WindowsRegistryEntry {
        override fun match(filter: WindowsRegistrySearchFilter): Boolean = filter.accept(this)
    }
}

enum class WindowsRegistryValueType(val id: String) {
    Str("REG_SZ"),
    Bin("REG_BINARY"),
    Int("REG_DWORD"),
    Long("REG_QWORD"),
    Text("REG_MULTI_SZ"),
    ExpandText("REG_EXPAND_SZ");

    companion object {
        fun tryParse(id: String): WindowsRegistryValueType? {
            return entries.singleOrNull { it.id.equals(id, true) }
        }
    }
}
