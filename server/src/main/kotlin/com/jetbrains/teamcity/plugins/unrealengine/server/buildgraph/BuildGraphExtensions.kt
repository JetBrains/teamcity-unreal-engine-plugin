package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

// TODO: add cycle check
fun <T> BuildGraph<T>.topologicalSort(): List<T> =
    buildList {
        val visited = mutableMapOf(*adjacencyList.map { it.key to false }.toTypedArray())

        fun dfs(node: T) {
            visited[node] = true
            adjacencyList[node]!!.forEach {
                if (visited[it] == false) {
                    dfs(it)
                }
            }
            add(node)
        }

        adjacencyList.forEach {
            if (visited[it.key] == false) {
                dfs(it.key)
            }
        }
    }.reversed()
