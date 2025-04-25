package dev.danielmillar.slimelink.util

import ch.njol.skript.Skript
import com.infernalsuite.asp.api.world.SlimeWorld
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

    fun teleportPlayersAndUnloadWorld(worldName: String, bukkitWorld: World, worldData: WorldData, teleportTarget: Location) {
        val playersInWorld = bukkitWorld.players
        val futures = playersInWorld.map { it.teleportAsync(teleportTarget) }
        val allOfFuture = CompletableFuture.allOf(*futures.toTypedArray())

        allOfFuture.thenRun {
            val success = unloadWorld(worldName, bukkitWorld, worldData)
            handleUnloadResult(worldName, success)
        }
    }
}