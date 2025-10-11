package dev.danielmillar.slimelink.slime

import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.loaders.file.FileLoader
import com.infernalsuite.asp.loaders.mongo.MongoLoader
import com.infernalsuite.asp.loaders.mysql.MysqlLoader
import dev.danielmillar.slimelink.config.ConfigManager
import java.io.File

enum class SlimeLoaderTypeEnum(val loaderId: String) {
    FILE("file") {
        override fun createLoader(): SlimeLoader {
            val fileConfig = ConfigManager.getDatasourcesConfig().getFileConfig()
            return FileLoader(File(fileConfig.getPath()))
        }
    },
    MYSQL("mysql") {
        override fun createLoader(): SlimeLoader {
            val mysqlConfig = ConfigManager.getDatasourcesConfig().getMysqlConfig()
            if (!mysqlConfig.isEnabled()) {
                throw IllegalStateException("MySQL datasource is not enabled in the configuration.")
            }
            return MysqlLoader(
                mysqlConfig.url,
                mysqlConfig.getHost(),
                mysqlConfig.getPort(),
                mysqlConfig.getDatabase(),
                mysqlConfig.isUseSsl(),
                mysqlConfig.getUsername(),
                mysqlConfig.getPassword()
            )
        }
    },
    MONGODB("mongodb") {
        override fun createLoader(): SlimeLoader {
            val mongoConfig = ConfigManager.getDatasourcesConfig().getMongoDbConfig()
            if (!mongoConfig.isEnabled()) {
                throw IllegalStateException("MongoDB datasource is not enabled in the configuration.")
            }
            return MongoLoader(
                mongoConfig.getDatabase(),
                mongoConfig.getCollection(),
                mongoConfig.getUsername(),
                mongoConfig.getPassword(),
                mongoConfig.getAuthSource(),
                mongoConfig.getHost(),
                mongoConfig.getPort(),
                mongoConfig.getUri()
            )
        }
    };
    abstract fun createLoader(): SlimeLoader
    companion object {
        fun fromId(loaderId: String): SlimeLoaderTypeEnum? {
            return entries.find { it.loaderId.equals(loaderId, ignoreCase = true) }
        }
    }
}