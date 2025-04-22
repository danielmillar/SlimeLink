package dev.danielmillar.slimelink.config

import org.slf4j.LoggerFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.io.IOException

@ConfigSerializable
data class WorldsConfig(
    @Setting("worlds")
    private val worlds: MutableMap<String, WorldData> = mutableMapOf()
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorldsConfig::class.java)
    }

    fun getWorlds(): Map<String, WorldData> = worlds.toMap()
    fun getWorld(worldName: String): WorldData? = worlds[worldName]
    fun setWorld(worldName: String, worldData: WorldData) {
        worlds[worldName] = worldData
    }
    fun removeWorld(worldName: String): WorldData? = worlds.remove(worldName)
    fun hasWorld(worldName: String): Boolean = worlds.containsKey(worldName)
    fun save() {
        try {
            ConfigManager.saveWorldConfig()
        } catch (ex: IOException) {
            logger.error("Failed to save worlds config", ex)
        }
    }
}