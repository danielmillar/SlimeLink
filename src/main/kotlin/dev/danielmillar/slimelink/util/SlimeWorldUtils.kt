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
     * Throws if a Bukkit world with that name is not loaded.
     * Returns the non-null World on success.
     */
    fun requireWorldLoaded(
        name: String,
        message: String = "World '$name' is not loaded"
    ): World =
        requireNotNull(Bukkit.getWorld(name)) { message }

    /**
     * Throws if a Bukkit world with that name is loaded.
     * @param message the exception message to throw when the world exists.
     */
    fun requireWorldNotLoaded(
        name: String,
        message: String = "World '$name' is already loaded!"
    ) {
        require(Bukkit.getWorld(name) == null) { message }
    }

    /**
     * Throws if a Bukkit world with that name is already loaded.
     */
    fun requireWorldNotExists(
        name: String,
        message: String = "A loaded world with that name already exists!"
    ) {
        require(Bukkit.getWorld(name) == null) { message }
    }

    /**
     * Throws if a world with that name is already in the config.
     */
    fun requireWorldDataNotExists(
        worldName: String,
        message: String = "World $worldName already exists in config"
    ) {
        val worldData = ConfigManager.getWorldConfig().getWorld(worldName)
        require(worldData == null) { message }
    }

    /**
     * Throws if a world with that name is not in the config.
     * Returns the non-null WorldData on success.
     */
    fun requireWorldDataExists(
        worldName: String,
        message: String = "World $worldName cannot be found in config"
    ): WorldData =
        requireNotNull(ConfigManager.getWorldConfig().getWorld(worldName)) {
            message
        }

    /**
     * Ensures a SlimeLoader for the given type is registered and returns it.
     */
    fun requireLoader(
        type: SlimeLoaderTypeEnum,
        message: String = "Loader '${type.name}' is not registered. Please initialize it first."
    ): SlimeLoader =
        requireNotNull(SlimeManager.getLoader(type)) {
            message
        }

    /**
     * Creates and saves a new Slime world entirely off the main thread,
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

    fun loadWorldAsync(
        worldName: String,
        loader: SlimeLoader,
        readOnly: Boolean,
        properties: SlimePropertyMap
    ) {
        val plugin = SlimeLink.getInstance()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val time = measureTimeMillis {
                val slimeWorld = SlimeLink.getASP().readWorld(loader, worldName, readOnly, properties)

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    SlimeLink.getASP().loadWorld(slimeWorld, true)
                })
            }
            Skript.info("Successfully loaded world '$worldName' in ${time}ms")
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