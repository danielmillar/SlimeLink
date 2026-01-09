package dev.danielmillar.slimelink.util

import ch.njol.skript.Skript
import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.api.world.SlimeWorld
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.SlimeLink
import org.bukkit.Bukkit
import org.bukkit.Bukkit.unloadWorld
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

object SlimeWorldUtils {

	/**
	 * Validates a world name to ensure it matches the regex pattern.
	 * @param name the name to validate
	 * @throws IllegalArgumentException if the name is invalid
	 */
	fun validateWorldName(name: String) {
		require(name.matches(Regex("^[a-z0-9/._-]+\$"))) {
			"World name '$name' is invalid. Only lowercase letters, numbers, hyphens, underscores, periods, and slashes are allowed."
		}
	}

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
	 * Creates a newSlime world on the main thread without saving or loading it.
	 *
	 * @param worldName the unique name of the world to create
	 * @param properties the [SlimePropertyMap] used for creation
	 * @param loader the [SlimeLoader] to handle storage
	 * @param readOnly whether the new world should be marked read-only
	 * @return the in-memory representation [SlimeWorld]
	 */
	fun createWorldSync(
		worldName: String,
		properties: SlimePropertyMap,
		loader: SlimeLoader,
		readOnly: Boolean
	): SlimeWorld? {
		return try {
			var slimeWorld: SlimeWorld? = null
			val time = measureTimeMillis {
				slimeWorld = SlimeLink.asp.createEmptyWorld(
					worldName,
					readOnly,
					properties,
					loader
				)
			}
			Skript.info("Successfully created world '$worldName' in ${time}ms")
			slimeWorld
		} catch (e: Exception) {
			Skript.error("Failed to create world '$worldName': ${e.message}")
			null
		}
	}

	/**
	 * Creates and saves a new Slime world off the main thread without loading it.
	 *
	 * @param worldName the unique name of the world to create
	 * @param properties the [SlimePropertyMap] used for creation
	 * @param loader the [SlimeLoader] to handle storage
	 * @param readOnly whether the new world should be marked read-only
	 */
	fun createWorldAsync(
		worldName: String,
		properties: SlimePropertyMap,
		loader: SlimeLoader,
		readOnly: Boolean
	) {
		val plugin = SlimeLink.instance
		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			try {
				val time = measureTimeMillis {
					val slimeWorld = SlimeLink.asp.createEmptyWorld(
						worldName,
						readOnly,
						properties,
						loader
					)
					SlimeLink.asp.saveWorld(slimeWorld)
				}

				Skript.info("Successfully created world '$worldName' in ${time}ms")
			} catch (e: Exception) {
				Skript.error("Failed to create world '$worldName': ${e.message}")
			}
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
	fun loadWorldSync(
		worldName: String,
		loader: SlimeLoader,
		readOnly: Boolean,
		properties: SlimePropertyMap
	) {
		val plugin = SlimeLink.instance
		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			try {
				val time = measureTimeMillis {
					val slimeWorld = SlimeLink.asp.readWorld(loader, worldName, readOnly, properties)

					Bukkit.getScheduler().runTask(plugin, Runnable {
						SlimeLink.asp.loadWorld(slimeWorld, true)
					})
				}
				Skript.info("Successfully loaded world '$worldName' in ${time}ms")
			} catch (e: Exception) {
				Skript.error("Failed to load world '$worldName': ${e.message}")
			}
		})
	}

	/**
	 * Loads a Slime World on main thread
	 *
	 * @param world the slime world object to load
	 */
	fun loadWorldSync(
		world: SlimeWorld,
	) {
		try {
			val time = measureTimeMillis {
				SlimeLink.asp.loadWorld(world, true)
			}
			Skript.info("Successfully loaded world '${world.name}' in ${time}ms")
		} catch (e: Exception) {
			Skript.error("Failed to load world '${world.name}': ${e.message}")
		}
	}

	/**
	 * Imports a vanilla world off the main thread, then loads it on the main thread.
	 *
	 * @param vanillaWorldPath the [File] path to the vanilla world folder
	 * @param slimeWorldName the unique name of the new Slime world
	 * @param loader the [SlimeLoader] to handle storage
	 */
	fun importSlimeWorldFromVanillaWorld(
		vanillaWorldPath: File,
		slimeWorldName: String,
		loader: SlimeLoader
	) {
		val plugin = SlimeLink.instance

		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			try {
				val time = measureTimeMillis {
					val slimeWorld = SlimeLink.asp.readVanillaWorld(vanillaWorldPath, slimeWorldName, loader)
					SlimeLink.asp.saveWorld(slimeWorld)

					Bukkit.getScheduler().runTask(plugin, Runnable {
						SlimeLink.asp.loadWorld(slimeWorld, true)
					})
				}

				Skript.info("Successfully imported world '$slimeWorldName' from '$vanillaWorldPath' in ${time}ms")
			} catch (e: Exception) {
				Skript.error("Failed to import world '$slimeWorldName': ${e.message}")
			}
		})
	}

	/**
	 * Clones a Slime world synchronously and returns the cloned world without saving.
	 *
	 * @param sourceWorldName the unique name of the source world to clone from
	 * @param targetWorldName the unique name of the new cloned world
	 * @param loader the [SlimeLoader] to handle storage
	 * @param readOnly whether the cloned world should be marked read-only
	 * @param properties the [SlimePropertyMap] for reading the source world
	 * @return the cloned [SlimeWorld] or null if cloning failed
	 */
	fun cloneWorldSync(
		sourceWorldName: String,
		targetWorldName: String,
		loader: SlimeLoader?,
		readOnly: Boolean,
		properties: SlimePropertyMap,
		storeWithLoader: Boolean = true
	): SlimeWorld? {
		return try {
			var clonedWorld: SlimeWorld? = null
			val time = measureTimeMillis {
				val sourceWorld = if (loader != null) {
					SlimeLink.asp.readWorld(loader, sourceWorldName, readOnly, properties)
				} else {
					SlimeLink.asp.getLoadedWorld(sourceWorldName)
						?: throw IllegalStateException("World '$sourceWorldName' is not loaded. Provide a loader to read from storage.")
				}
				clonedWorld = if (loader != null && storeWithLoader) {
					sourceWorld.clone(targetWorldName, loader)
				} else {
					sourceWorld.clone(targetWorldName)
				}
			}
			Skript.info("Successfully cloned world '$sourceWorldName' to '$targetWorldName' in ${time}ms")
			clonedWorld
		} catch (e: Exception) {
			Skript.error("Failed to clone world '$sourceWorldName' to '$targetWorldName': ${e.message}")
			null
		}
	}

	/**
	 * Clones a Slime world off the main thread and saves it to the loader.
	 *
	 * @param sourceWorldName the unique name of the source world to clone from
	 * @param targetWorldName the unique name of the new cloned world
	 * @param loader the [SlimeLoader] to handle storage
	 * @param readOnly whether the cloned world should be marked read-only
	 * @param properties the [SlimePropertyMap] for reading the source world
	 */
	fun cloneWorldAsync(
		sourceWorldName: String,
		targetWorldName: String,
		loader: SlimeLoader,
		readOnly: Boolean,
		properties: SlimePropertyMap
	) {
		val plugin = SlimeLink.instance
		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			try {
				val time = measureTimeMillis {
					val sourceWorld = SlimeLink.asp.readWorld(loader, sourceWorldName, readOnly, properties)
					val clonedWorld = sourceWorld.clone(targetWorldName, loader)
					SlimeLink.asp.saveWorld(clonedWorld)
				}
				Skript.info("Successfully cloned world '$sourceWorldName' to '$targetWorldName' in ${time}ms")
			} catch (e: Exception) {
				Skript.error("Failed to clone world '$sourceWorldName' to '$targetWorldName': ${e.message}")
			}
		})
	}

	/**
	 * Deletes a Slime world off the main thread.
	 *
	 * @param worldName the unique name of the world to delete
	 * @param loader the [SlimeLoader] to handle deletion
	 */
	fun deleteWorldAsync(
		worldName: String,
		loader: SlimeLoader
	) {
		val plugin = SlimeLink.instance
		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			try {
				val time = measureTimeMillis {
					loader.deleteWorld(worldName)
				}
				Skript.info("Successfully deleted world '$worldName' in ${time}ms")
			} catch (e: Exception) {
				Skript.error("Failed to delete world '$worldName': ${e.message}")
			}
		})
	}

	/**
	 * Saves a Slime world off the main thread.
	 *
	 * @param worldName the unique name of the world
	 */
	fun saveWorldSync(
		worldName: String,
	) {
		val plugin = SlimeLink.instance
		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			val time = measureTimeMillis {
                val loadedWorld = SlimeLink.asp.getLoadedWorld(worldName) ?: return@Runnable
                SlimeLink.asp.saveWorld(loadedWorld)
			}
			Skript.info("Successfully saved world '$worldName' in ${time}ms")
		})
	}

	/**
	 * Unloads a Slime world on the main thread.
	 *
	 * @param worldName the unique name of the world to unload
	 * @param bukkitWorld the [World] instance to unload
	 * @param noSave if true, discards changes; if false, saves before unloading
	 */
	fun unloadWorldSync(
		worldName: String,
		bukkitWorld: World,
		noSave: Boolean
	) {
		val plugin = SlimeLink.instance

		var attempts = 0
		val maxAttempts = 10
		object : BukkitRunnable() {
			override fun run() {
				if (Bukkit.isTickingWorlds()) {
					if (++attempts >= maxAttempts) {
						Skript.error("Failed to unload world '$worldName' after waiting for ticking to stop.")
						cancel()
					}
					return
				}

				cancel()
				var success: Boolean
				val time = measureTimeMillis {
					success = unloadWorld(bukkitWorld, !noSave)
				}

				if (success) {
					Skript.info("Successfully unloaded world '$worldName' in ${time}ms")
				} else {
					Skript.error("Failed to unload world '$worldName' in ${time}ms, it may still be loaded")
				}
			}
		}.runTaskTimer(plugin, 0L, 5L)
	}

	/**
	 * Attempts to unload a world by either unloading if empty or teleporting players then unloading.
	 *
	 * @param worldName the unique name of the world
	 * @param bukkitWorld the [World] instance to unload
	 * @param noSave if true, discards changes; if false, saves before unloading
	 * @param shouldTeleport whether to teleport players before unloading
	 * @param teleportTarget the [Location] to teleport players to (required if [shouldTeleport] is true)
	 * @throws IllegalArgumentException if players exist and [shouldTeleport] is false, or if teleportTarget is null
	 */
	fun unloadWithOptionalTeleport(
		worldName: String,
		bukkitWorld: World,
		noSave: Boolean,
		shouldTeleport: Boolean,
		teleportTarget: Location?
	) {
		val players = bukkitWorld.players
		if (players.isEmpty()) {
			unloadWorldSync(worldName, bukkitWorld, noSave)
			return
		}

		require(shouldTeleport) {
			"Players in world '$worldName'; cannot unload without removing them"
		}

		val target = requireNotNull(teleportTarget) {
			"Teleport target location is null, unable to unload world '$worldName'"
		}

		teleportPlayersAndUnloadWorld(worldName, bukkitWorld, noSave, target)
	}

	/**
	 * Teleports all players in a world to a specified location and then unloads the world.
	 *
	 * @param worldName the unique name of the world
	 * @param bukkitWorld the [World] instance containing players
	 * @param noSave if true, discards changes; if false, saves before unloading
	 * @param teleportTarget the [Location] to teleport players to
	 */
	fun teleportPlayersAndUnloadWorld(
		worldName: String,
		bukkitWorld: World,
		noSave: Boolean,
		teleportTarget: Location
	) {
		val playersInWorld = bukkitWorld.players
		val completableFuture =
			CompletableFuture.allOf(*playersInWorld.map { it.teleportAsync(teleportTarget) }.toTypedArray())

		completableFuture.thenRun {
			unloadWorldSync(worldName, bukkitWorld, noSave)
		}.exceptionally {
			Skript.error("Failed to teleport players and unload world '$worldName': ${it.message}")
			null
		}
	}
}
