package dev.danielmillar.slimelink.util

import ch.njol.skript.Skript
import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.config.WorldData
import dev.danielmillar.slimelink.events.SlimeWorldLoadEvent
import dev.danielmillar.slimelink.events.SlimeWorldUnloadEvent
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.slime.SlimeManager
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import org.bukkit.Bukkit
import org.bukkit.Bukkit.unloadWorld
import org.bukkit.Location
import org.bukkit.World
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

object SlimeWorldUtils {

    /**
     * Ensures that a Bukkit world with the given name is loaded.
     *
     * @param name the name of the world to check
     * @param message the exception message if the world is not loaded
     * @return the loaded Bukkit [World]
     * @throws IllegalArgumentException if no world with [name] is loaded
     */
    fun requireWorldLoaded(
        name: String,
        message: String = "World '$name' is not loaded"
    ): World =
        requireNotNull(Bukkit.getWorld(name)) { message }

    /**
     * Ensures that no Bukkit world with the given name is loaded.
     * Use in creation flows to prevent name collisions.
     *
     * @param name the name of the world to check
     * @param message the exception message if the world is already loaded
     * @throws IllegalArgumentException if a world with [name] is already loaded
     */
    fun requireWorldNotLoaded(
        name: String,
        message: String = "World '$name' is already loaded!"
    ) {
        require(Bukkit.getWorld(name) == null) { message }
    }

    /**
     * Ensures that no loaded world with the given name exists.
     * Alias for [requireWorldNotLoaded] with a different default message.
     *
     * @param name the name of the world to check
     * @param message the exception message if the world is already loaded
     * @throws IllegalArgumentException if a loaded world with [name] exists
     */
    fun requireWorldNotExists(
        name: String,
        message: String = "A loaded world with that name already exists!"
    ) {
        require(Bukkit.getWorld(name) == null) { message }
    }

    /**
     * Ensures that no world data for the given name exists in the plugin config.
     *
     * @param worldName the name of the world to check in config
     * @param message the exception message if config entry exists
     * @throws IllegalArgumentException if config already contains [worldName]
     */
    fun requireWorldDataNotExists(
        worldName: String,
        message: String = "World $worldName already exists in config"
    ) {
        val worldData = ConfigManager.getWorldConfig().getWorld(worldName)
        require(worldData == null) { message }
    }

    /**
     * Ensures that a world config entry exists for the given name.
     *
     * @param worldName the name of the world to fetch from config
     * @param message the exception message if config entry is missing
     * @return the plugin [WorldData] for [worldName]
     * @throws IllegalArgumentException if no config entry for [worldName]
     */
    fun requireWorldDataExists(
        worldName: String,
        message: String = "World $worldName cannot be found in config"
    ): WorldData =
        requireNotNull(ConfigManager.getWorldConfig().getWorld(worldName)) {
            message
        }

    /**
     * Ensures a [SlimeLoader] for the given type is registered.
     *
     * @param type the [SlimeLoaderTypeEnum] to retrieve
     * @param message the exception message if loader is not registered
     * @return the non-null [SlimeLoader] instance
     * @throws IllegalArgumentException if no loader for [type] is registered
     */
    fun requireLoader(
        type: SlimeLoaderTypeEnum,
        message: String = "Loader '${type.name}' is not registered. Please initialize it first."
    ): SlimeLoader =
        requireNotNull(SlimeManager.getLoader(type)) {
            message
        }

    /**
     * Creates and saves a new Slime world off the main thread, then loads it and updates config on the main thread.
     *
     * @param worldName the unique name of the world to create
     * @param properties the [SlimePropertyMap] used for creation
     * @param loader the [SlimeLoader] to handle storage
     * @param loaderName the identifier used in the [WorldData.source]
     * @param readOnly whether the new world should be marked read-only
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
                    val world = SlimeLink.getASP().loadWorld(slimeWorld, true)
                    SlimeWorldLoadEvent(world.bukkitWorld).callEvent()

                    val worldData = WorldData(source = loaderName, readOnly = readOnly)
                    ConfigManager.getWorldConfig().setWorld(worldName, worldData)
                    ConfigManager.saveWorldConfig()
                })
            }

            Skript.info("Successfully created world '$worldName' in ${time}ms")
        })
    }

    /**
     * Reads a Slime world off the main thread, then loads it on the main thread.
     *
     * @param worldName the unique name of the world to read
     * @param loader the [SlimeLoader] used for reading
     * @param readOnly whether to open the world in read-only mode
     * @param properties the [SlimePropertyMap] for reading
     */
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
                    val world = SlimeLink.getASP().loadWorld(slimeWorld, true)
                    SlimeWorldLoadEvent(world.bukkitWorld).callEvent()
                })
            }
            Skript.info("Successfully loaded world '$worldName' in ${time}ms")
        })
    }

    /**
     * Deletes a Slime world and removes it from config off the main thread.
     *
     * @param worldName the unique name of the world to delete
     * @param loader the [SlimeLoader] to handle deletion
     */
    fun deleteWorldAsync(
        worldName: String,
        loader: SlimeLoader
    ) {
        val plugin = SlimeLink.getInstance()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val time = measureTimeMillis {
                loader.deleteWorld(worldName)

                ConfigManager.getWorldConfig().removeWorld(worldName)
                ConfigManager.saveWorldConfig()
            }
            Skript.info("Successfully deleted world '$worldName' in ${time}ms")
        })
    }

    /**
     * Saves a Bukkit world and updates config on the main thread.
     *
     * @param worldBukkit the [World] instance to save
     * @param worldName the unique name of the world
     * @param worldData the updated [WorldData] to store
     */
    fun saveWorldSync(
        worldBukkit: World,
        worldName: String,
        worldData: WorldData,
    ) {
        val plugin = SlimeLink.getInstance()
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val time = measureTimeMillis {
                worldBukkit.save()

                ConfigManager.getWorldConfig().setWorld(worldName, worldData)
                ConfigManager.saveWorldConfig()
            }
            Skript.info("Successfully saved world '$worldName' in ${time}ms")
        })
    }

    /**
     * Unloads a Slime world and updates config on the main thread.
     *
     * @param worldName the unique name of the world to unload
     * @param bukkitWorld the [World] instance to unload
     * @param worldData the [WorldData] for this world
     */
    fun unloadWorldSync(
        worldName: String,
        bukkitWorld: World,
        worldData: WorldData,
    ) {
        val plugin = SlimeLink.getInstance()
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val time = measureTimeMillis {
                if (worldData.isReadOnly()) {
                    SlimeWorldUnloadEvent(bukkitWorld).callEvent()
                    unloadWorld(bukkitWorld, false)
                } else {
                    ConfigManager.getWorldConfig().setWorld(worldName, worldData)
                    ConfigManager.saveWorldConfig()

                    SlimeWorldUnloadEvent(bukkitWorld).callEvent()
                    unloadWorld(bukkitWorld, true)
                }
            }
            Skript.info("Successfully unloaded world '$worldName' in ${time}ms")
        })
    }

    /**
     * Attempts to unload a world by either unloading if empty or teleporting players then unloading.
     *
     * @param worldName the unique name of the world
     * @param bukkitWorld the [World] instance to unload
     * @param worldData the [WorldData] for this world
     * @param shouldTeleport whether to teleport players before unloading
     * @param teleportTarget the [Location] to teleport players to (required if [shouldTeleport] is true)
     * @throws IllegalArgumentException if players exist and [shouldTeleport] is false, or if teleportTarget is null
     */
    fun unloadWithOptionalTeleport(
        worldName: String,
        bukkitWorld: World,
        worldData: WorldData,
        shouldTeleport: Boolean,
        teleportTarget: Location?
    ) {
        val players = bukkitWorld.players
        if (players.isEmpty()) {
            unloadWorldSync(worldName, bukkitWorld, worldData)
            return
        }

        require(shouldTeleport) {
            "Players in world '$worldName'; cannot unload without removing them"
        }

        val target = requireNotNull(teleportTarget) {
            "Teleport target location is null, unable to unload world '$worldName'"
        }

        teleportPlayersAndUnloadWorld(worldName, bukkitWorld, worldData, target)
    }

    /**
     * Teleports all players in a world to a specified location and then unloads the world.
     *
     * @param worldName the unique name of the world
     * @param bukkitWorld the [World] instance containing players
     * @param worldData the [WorldData] for this world
     * @param teleportTarget the [Location] to teleport players to
     */
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
            unloadWorldSync(worldName, bukkitWorld, worldData)
        }
    }
}
