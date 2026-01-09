package dev.danielmillar.slimelink.slime

import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.loaders.file.FileLoader
import com.infernalsuite.asp.loaders.mongo.MongoLoader
import com.infernalsuite.asp.loaders.mysql.MysqlLoader
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.SourcesConfig
import java.io.File

enum class SlimeLoaderType(val id: String) {
    FILE("file") {
        override fun createLoader(): SlimeLoader {
            val config = SlimeLink.instance.configManager.get<SourcesConfig>().file
            return FileLoader(File(config.path))
        }
    },
    
    MYSQL("mysql") {
        override fun createLoader(): SlimeLoader {
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
        override fun createLoader(): SlimeLoader {
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

    abstract fun createLoader(): SlimeLoader
}