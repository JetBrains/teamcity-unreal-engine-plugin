package com.jetbrains.teamcity.plugins.framework.resource.location

import arrow.core.Either
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.CommandLineRunner
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.LinuxResourceLocationContext
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.LinuxResourceLocationQuery
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.MacOsResourceLocationContext
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.MacOsResourceLocationQuery
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.ResourceLocationQuery
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.UniversalResourceLocationContext
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.UniversalResourceLocationQuery
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.WindowsResourceLocationContext
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.WindowsResourceLocationQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.nio.file.Path

sealed interface ResourceLocationResult<out T> {
    data class Success<T>(val data: T) : ResourceLocationResult<T>
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ResourceLocationResult<Nothing>
}

class ResourceLocationContext : WindowsResourceLocationContext, LinuxResourceLocationContext, MacOsResourceLocationContext,
    UniversalResourceLocationContext {
        override fun pathOf(fileName: String): Path = Path.of(fileName)
        override val commandLineRunner = CommandLineRunner()
    }

class QueryBuilder<T>(
    private val os: OSType,
) {
    private val queries = mutableListOf<ResourceLocationQuery<in ResourceLocationContext, T>>()

    fun macos(vararg init: MacOsResourceLocationQuery<*>.() -> MacOsResourceLocationQuery<T>) {
        if (os == OSType.MacOs) {
            queries.addAll(init.map {
                val query = MacOsResourceLocationQuery { }
                val r = it(query)
                r
            })
        }
    }

    fun linux(vararg init: LinuxResourceLocationQuery<*>.() -> LinuxResourceLocationQuery<T>) {
        if (os == OSType.Linux) {
            queries.addAll(init.map {
                val query = LinuxResourceLocationQuery { }
                val r = it(query)
                r
            })
        }
    }

    fun windows(vararg init: WindowsResourceLocationQuery<*>.() -> WindowsResourceLocationQuery<T>) {
        if (os == OSType.Windows) {
            queries.addAll(init.map {
                val query = WindowsResourceLocationQuery { }
                val r = it(query)
                r
            })
        }
    }

    fun anyOS(vararg init: UniversalResourceLocationQuery<*>.() -> UniversalResourceLocationQuery<T>) {
        queries.addAll(init.map {
            val query = UniversalResourceLocationQuery { }
            val r = it(query)
            r
        })
    }

    context(ResourceLocationContext)
    internal suspend fun executeQueries(): List<ResourceLocationResult<T>> = coroutineScope {
        queries.map {
            async(Dispatchers.IO) {
                val res = either {
                    it.execute()
                }

                when (res) {
                    is Either.Left -> res.value
                    is Either.Right -> ResourceLocationResult.Success(res.value)
                }
            }
        }.awaitAll()
    }
}

class ResourceLocator(
    private val environment: Environment,
    private val context: ResourceLocationContext,
) {

    private val logger = TeamCityLoggers.get<ResourceLocator>()

    init {
        logger.info("The operating system the location process is running on is ${environment.osType}")
    }

    suspend fun <T> locateResources(init: QueryBuilder<T>.() -> Unit): List<ResourceLocationResult<T>> {
        with(context) {
            return QueryBuilder<T>(environment.osType)
                .apply(init)
                .executeQueries()
        }
    }
}
