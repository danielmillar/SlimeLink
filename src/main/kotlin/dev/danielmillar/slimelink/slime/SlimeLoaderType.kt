package dev.danielmillar.slimelink.slime

import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.api.loaders.UpdatableLoader
import com.infernalsuite.asp.loaders.file.FileLoader
import com.infernalsuite.asp.loaders.mongo.MongoLoader
import com.infernalsuite.asp.loaders.mysql.MysqlLoader
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.SourcesConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

enum class SlimeLoaderType(val id: String) {
    FILE("file") {
        override fun createLoaderInternal(): SlimeLoader {
            val config = SlimeLink.instance.configManager.get<SourcesConfig>().file
            return FileLoader(File(config.path))
        }
    },
    
    MYSQL("mysql") {
        override fun createLoaderInternal(): SlimeLoader {
            val config = SlimeLink.instance.configManager.get<SourcesConfig>().mysql
            require(config.enabled) { 
                "MySQL datasource is not enabled in the configuration." 
            }
            return MysqlLoader(
                config.url,
                config.host,
                config.port,
                config.database,
                config.useSsl,
                config.username,
                config.password
            )
        }
    },
    
    MONGODB("mongodb") {
        override fun createLoaderInternal(): SlimeLoader {
            val config = SlimeLink.instance.configManager.get<SourcesConfig>().mongodb
            require(config.enabled) {
                "MongoDB datasource is not enabled in the configuration." 
            }
            return MongoLoader(
                config.database,
                config.collection,
                config.username,
                config.password,
                config.authSource,
                config.host,
                config.port,
                config.uri
            )
        }
    };

    fun createLoader(): SlimeLoader {
        return cachedLoaders.computeIfAbsent(this) { loaderType ->
            val loader = try {
                loaderType.createLoaderInternal()
            } catch (exception: IllegalStateException) {
                throw exception
            } catch (exception: Exception) {
                throw IllegalStateException(
                    "Failed to initialize ${loaderType.id} datasource: ${exception.message ?: "unknown error"}",
                    exception
                )
            }
            updateLoaderIfRequired(loaderType.id, loader)
        }
    }

    protected abstract fun createLoaderInternal(): SlimeLoader

    companion object {
        private val cachedLoaders = ConcurrentHashMap<SlimeLoaderType, SlimeLoader>()

        fun clearCache() {
            cachedLoaders.clear()
        }

        private fun updateLoaderIfRequired(loaderName: String, loader: SlimeLoader): SlimeLoader {
            if (loader !is UpdatableLoader) {
                return loader
            }

            try {
                loader.update()
                return loader
            } catch (exception: UpdatableLoader.NewerStorageException) {
                throw IllegalStateException(
                    "Datasource '$loaderName' is newer than this loader supports " +
                        "(storage=${exception.storageVersion}, supported=${exception.implementationVersion}).",
                    exception
                )
            } catch (exception: IOException) {
                throw IllegalStateException("Failed to update '$loaderName' datasource: ${exception.message}", exception)
            } catch (exception: Exception) {
                throw IllegalStateException("Failed to update '$loaderName' datasource: ${exception.message}", exception)
            }
        }
    }
}
