package dev.danielmillar.slimelink.util

import ch.njol.skript.Skript
import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.api.world.SlimeWorld
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.config.WorldData
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

object SlimeWorldUtils {

    /**
     * Throws if no Bukkit world with that name is loaded.
     * Returns the non-null World on success.
     */
    fun requireWorldExists(name: String): World =
        requireNotNull(Bukkit.getWorld(name)) { "World '$name' is not loaded" }

    /**
     * Throws if a Bukkit world with that name *is* already loaded.
     * Use when you’re creating a new world and want to prevent name clashes.
     */
    fun requireWorldNotExists(name: String) {
        require(Bukkit.getWorld(name) == null) { "A world with that name already exists!" }
    }

    /**
     * Throws if a world with that name is already in the config.
     * Use when you’re creating a new world and want to prevent name clashes.
     */
    fun requireWorldDataNotExists(worldName: String) {
        val worldData = ConfigManager.getWorldConfig().getWorld(worldName)
        require(worldData == null) { "World $worldName already exists in config" }
    }

    /**
     * Ensures a SlimeLoader for the given type is registered and returns it.
     */
    fun requireLoader(type: SlimeLoaderTypeEnum): SlimeLoader =
        requireNotNull(SlimeManager.getLoader(type)) {
            "Loader '${type.name}' is not registered. Please initialize it first."
        }

    /**
     * Creates, saves a new Slime world entirely off the main thread,
     * then loads the world and updates the config on the main thread.
     */
    fun createAndLoadWorldAsync(
        worldName: String,
        properties: SlimePropertyMap,
        loader: SlimeLoader,
        loaderName: String,
        readOnly: Boolean
    ) {
        val plugin = SlimeLink.getInstance()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val time = measureTimeMillis {
                val slimeWorld = SlimeLink.getASP().createEmptyWorld(
                    worldName,
                    readOnly,
                    properties,
                    loader
                )
                SlimeLink.getASP().saveWorld(slimeWorld)

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    SlimeLink.getASP().loadWorld(slimeWorld, true)

                    val worldData = WorldData(source = loaderName, readOnly = readOnly)
                    ConfigManager.getWorldConfig().setWorld(worldName, worldData)
                    ConfigManager.saveWorldConfig()
                })
            }

            Skript.info("Successfully created world '$worldName' in ${time}ms")
        })
    }

    // Older functions below to be replaced during refactoring

    fun validateWorldByName(worldName: String): World? {
        val bukkitWorld = Bukkit.getWorld(worldName)
        if (bukkitWorld == null) {
            Skript.error("World $worldName is not loaded!")
            return null
        }
        return bukkitWorld
    }

    fun getWorldData(worldName: String): WorldData? {
        val worldData = ConfigManager.getWorldConfig().getWorld(worldName)
        if (worldData == null) {
            Skript.error("World $worldName cannot be found in config")
            return null
        }
        return worldData
    }

    fun isWorldReadOnly(worldName: String, worldData: WorldData): Boolean {
        if (worldData.isReadOnly()) {
            Skript.warning("World $worldName readOnly property is true, can't save")
            return true
        }
        return false
    }

    fun validateLoader(loaderType: SlimeLoaderTypeEnum): Boolean {
        val loader = SlimeManager.getLoader(loaderType)
        if (loader == null) {
            Skript.error("Loader ${loaderType.name} is not registered. Please initialize it first.")
            return false
        }
        return true
    }

    fun saveWorld(worldName: String, worldData: WorldData): Long {
        return try {
            val timeTaken = measureTimeMillis {
                val loadedWorld = SlimeLink.getASP().getLoadedWorld(worldName) as SlimeWorld
                SlimeLink.getASP().saveWorld(loadedWorld)

                ConfigManager.getWorldConfig().setWorld(worldName, worldData)
                ConfigManager.saveWorldConfig()
            }

            Skript.info("World $worldName saved within $timeTaken ms!")
            timeTaken
        } catch (ex: Exception) {
            when (ex) {
                is IOException -> {
                    Skript.error("Failed to save world $worldName. Check logger for more information")
                    ex.printStackTrace()
                }

                else -> {
                    Skript.error("Failed to save world $worldName: ${ex.message}")
                    ex.printStackTrace()
                }
            }
            -1
        }
    }

    fun unloadWorld(worldName: String, bukkitWorld: World, worldData: WorldData): Boolean {
        return if (worldData.isReadOnly()) {
            Bukkit.unloadWorld(bukkitWorld, false)
        } else {
            try {
                val loadedWorld = SlimeLink.getASP().getLoadedWorld(worldName)
                SlimeLink.getASP().saveWorld(loadedWorld)

                ConfigManager.getWorldConfig().setWorld(worldName, worldData)
                ConfigManager.saveWorldConfig()

                Bukkit.unloadWorld(bukkitWorld, true)
            } catch (ex: Exception) {
                Skript.error("Failed to save world $worldName before unloading: ${ex.message}")
                ex.printStackTrace()
                false
            }
        }
    }

    fun handleUnloadResult(worldName: String, success: Boolean) {
        if (success) {
            Skript.info("World $worldName unloaded successfully")
        } else {
            Skript.error("World $worldName failed to unload")
        }
    }

    fun getTeleportTarget(customLocation: Location?): Location? {
        return if (customLocation != null) {
            customLocation
        } else {
            val defaultWorld = Bukkit.getWorlds().firstOrNull() ?: run {
                Skript.error("No default world found to teleport players to")
                return null
            }
            defaultWorld.spawnLocation
        }
    }

    fun teleportPlayersAndUnloadWorld(
        worldName: String,
        bukkitWorld: World,
        worldData: WorldData,
        teleportTarget: Location
    ) {
        val playersInWorld = bukkitWorld.players
        val futures = playersInWorld.map { it.teleportAsync(teleportTarget) }
        val allOfFuture = CompletableFuture.allOf(*futures.toTypedArray())

        allOfFuture.thenRun {
            val success = unloadWorld(worldName, bukkitWorld, worldData)
            handleUnloadResult(worldName, success)
        }
    }
}