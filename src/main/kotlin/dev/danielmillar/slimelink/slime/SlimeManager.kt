package dev.danielmillar.slimelink.slime

import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.loaders.file.FileLoader
import com.infernalsuite.asp.loaders.mongo.MongoLoader
import com.infernalsuite.asp.loaders.mysql.MysqlLoader
import com.mongodb.MongoException
import dev.danielmillar.slimelink.config.ConfigManager
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.SQLException

object SlimeManager {
    private val logger = LoggerFactory.getLogger(SlimeManager::class.java)

    private val loaders = mutableMapOf<SlimeLoaderTypeEnum, SlimeLoader>()

    fun registerLoader(type: SlimeLoaderTypeEnum): Boolean {
        val config = ConfigManager.getDatasourcesConfig()

        when (type) {
            SlimeLoaderTypeEnum.FILE -> {
                val fileConfig = config.getFileConfig()
                loaders.put(SlimeLoaderTypeEnum.FILE, FileLoader(File(fileConfig.getPath())))
            }

            SlimeLoaderTypeEnum.MYSQL -> {
                val mysqlConfig = config.getMysqlConfig()
                if (mysqlConfig.isEnabled()) {
                    try {
                        loaders.put(
                            SlimeLoaderTypeEnum.MYSQL, MysqlLoader(
                                mysqlConfig.url,
                                mysqlConfig.getHost(),
                                mysqlConfig.getPort(),
                                mysqlConfig.getDatabase(),
                                mysqlConfig.isUseSsl(),
                                mysqlConfig.getUsername(),
                                mysqlConfig.getPassword()
                            )
                        )
                    }catch (ex: SQLException) {
                        logger.error("Failed to connect to MySQL database", ex)
                        false
                    }
                }
            }

            SlimeLoaderTypeEnum.MONGODB -> {
                val mongoConfig = config.getMongoDbConfig()
                if (mongoConfig.isEnabled()) {
                    try {
                        loaders.put(SlimeLoaderTypeEnum.MONGODB, MongoLoader(
                            mongoConfig.getDatabase(),
                            mongoConfig.getCollection(),
                            mongoConfig.getUsername(),
                            mongoConfig.getPassword(),
                            mongoConfig.getAuthSource(),
                            mongoConfig.getHost(),
                            mongoConfig.getPort(),
                            mongoConfig.getUri()
                        ))
                    } catch (ex: MongoException) {
                        logger.error("Failed to connect to MongoDB database", ex)
                        false
                    }
                }
            }
        }

        return true
    }

    fun getLoader(type: SlimeLoaderTypeEnum): SlimeLoader? =
        loaders[type]

    fun unregisterLoader(type: SlimeLoaderTypeEnum): Boolean =
        loaders.remove(type) != null

    fun registeredTypes(): Set<SlimeLoaderTypeEnum> =
        loaders.keys
}