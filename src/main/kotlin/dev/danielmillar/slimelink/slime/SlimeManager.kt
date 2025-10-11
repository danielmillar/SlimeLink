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
        if (loaders.containsKey(type)) {
            logger.info("Loader for ${type.name} is already registered. Skipping re-registration.")
            return true
        }

        try {
            loaders[type] = type.createLoader()
            return true
        } catch (ex: Exception) {
            logger.error("Failed to register loader for ${type.name}", ex)
            return false
        }
    }

    fun getLoader(type: SlimeLoaderTypeEnum): SlimeLoader? =
        loaders[type]

    fun unregisterLoader(type: SlimeLoaderTypeEnum): Boolean =
        loaders.remove(type) != null

    fun registeredTypes(): Set<SlimeLoaderTypeEnum> =
        loaders.keys
}