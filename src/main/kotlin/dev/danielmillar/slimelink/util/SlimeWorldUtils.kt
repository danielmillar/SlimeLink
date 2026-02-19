package dev.danielmillar.slimelink.util

import ch.njol.skript.Skript
import com.infernalsuite.asp.api.exceptions.*
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
import java.io.IOException
import java.util.*
import java.util.concurrent.*
import kotlin.system.measureTimeMillis

object SlimeWorldUtils {

	private val worldNamePattern = Regex("^[a-z0-9/._-]+$")
	private val operationsInProgress: MutableSet<String> = ConcurrentHashMap.newKeySet()
	private const val SYNC_OPERATION_TIMEOUT_SECONDS = 30L

	/**
	 * Validates a world name to ensure it matches the regex pattern.
	 * @param name the name to validate
	 * @throws IllegalArgumentException if the name is invalid
	 */
	fun validateWorldName(name: String) {
		require(name.matches(worldNamePattern)) {
			"World name '$name' is invalid. Only lowercase letters, numbers, hyphens, underscores, periods, and slashes are allowed."
		}
	}

	fun userFacingError(throwable: Throwable): String {
        return when (val cause = unwrapException(throwable)) {
			is WorldAlreadyExistsException -> cause.message ?: "A world with that name already exists."
			is UnknownWorldException -> cause.message ?: "The requested world could not be found."
			is CorruptedWorldException -> cause.message ?: "The world appears to be corrupted."
			is NewerFormatException ->
				"This world was serialized with a newer Slime Format (${cause.message ?: "unknown"})."

			is InvalidWorldException -> cause.message ?: "The provided folder is not a valid vanilla world."
			is WorldLoadedException -> cause.message ?: "A world with that name is currently loaded."
			is WorldTooBigException -> cause.message ?: "The world is too large to be imported into Slime Format."
			is IOException -> "I/O error: ${cause.message ?: "see server logs for details"}"
			is IllegalArgumentException -> cause.message ?: "Invalid world operation."
			is IllegalStateException -> cause.message ?: "Operation cannot be completed right now."
			else -> cause.message ?: "Unexpected error occurred. Check server logs for details."
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
	): SlimeWorld {
		return withWorldLocks(listOf(worldName)) {
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
			checkNotNull(slimeWorld) { "Failed to create world '$worldName'." }
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
	): CompletableFuture<Unit> {
		return runAsyncLocked("create world '$worldName'", listOf(worldName)) {
			if (loader.worldExists(worldName)) {
				throw WorldAlreadyExistsException(worldName)
			}

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
		}
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
	): CompletableFuture<Unit> {
		return runAsyncLocked("load world '$worldName'", listOf(worldName)) {
			val time = measureTimeMillis {
				val slimeWorld = SlimeLink.asp.readWorld(loader, worldName, readOnly, properties)
				runSyncAndWait {
					SlimeLink.asp.loadWorld(slimeWorld, true)
				}
			}
			Skript.info("Successfully loaded world '$worldName' in ${time}ms")
		}
	}

	/**
	 * Loads a Slime World on main thread
	 *
	 * @param world the slime world object to load
	 */
	fun loadWorldSync(
		world: SlimeWorld,
	) {
		withWorldLocks(listOf(world.name)) {
			val time = measureTimeMillis {
				runSyncAndWait {
					SlimeLink.asp.loadWorld(world, true)
				}
			}
			Skript.info("Successfully loaded world '${world.name}' in ${time}ms")
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
	): CompletableFuture<Unit> {
		return runAsyncLocked("import world '$slimeWorldName'", listOf(slimeWorldName)) {
			val time = measureTimeMillis {
				val slimeWorld = SlimeLink.asp.readVanillaWorld(vanillaWorldPath, slimeWorldName, loader)
				SlimeLink.asp.saveWorld(slimeWorld)
				runSyncAndWait {
					SlimeLink.asp.loadWorld(slimeWorld, true)
				}
			}

			Skript.info("Successfully imported world '$slimeWorldName' from '$vanillaWorldPath' in ${time}ms")
		}
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
	): SlimeWorld {
		return withWorldLocks(listOf(sourceWorldName, targetWorldName)) {
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
			checkNotNull(clonedWorld) { "Failed to clone world '$sourceWorldName' to '$targetWorldName'." }
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
	): CompletableFuture<Unit> {
		return runAsyncLocked("clone world '$sourceWorldName' to '$targetWorldName'", listOf(sourceWorldName, targetWorldName)) {
			val time = measureTimeMillis {
				val sourceWorld = SlimeLink.asp.readWorld(loader, sourceWorldName, readOnly, properties)
				sourceWorld.clone(targetWorldName, loader)
			}
			Skript.info("Successfully cloned world '$sourceWorldName' to '$targetWorldName' in ${time}ms")
		}
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
	): CompletableFuture<Unit> {
		return runAsyncLocked("delete world '$worldName'", listOf(worldName)) {
			val time = measureTimeMillis {
				loader.deleteWorld(worldName)
			}
			Skript.info("Successfully deleted world '$worldName' in ${time}ms")
		}
	}

	/**
	 * Saves a Slime world off the main thread.
	 *
	 * @param worldName the unique name of the world
	 */
	fun saveWorldSync(
		worldName: String,
	): CompletableFuture<Unit> {
		return runAsyncLocked("save world '$worldName'", listOf(worldName)) {
			val loadedWorld = SlimeLink.asp.getLoadedWorld(worldName)
				?: throw IllegalStateException("World '$worldName' is no longer loaded.")
			val time = measureTimeMillis {
				SlimeLink.asp.saveWorld(loadedWorld)
			}
			Skript.info("Successfully saved world '$worldName' in ${time}ms")
		}
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
		val locks = acquireWorldLocks(listOf(worldName))
		unloadWorldSync(worldName, bukkitWorld, noSave, locks)
	}

	private fun unloadWorldSync(
		worldName: String,
		bukkitWorld: World,
		noSave: Boolean,
		locks: List<String>
	) {
		val plugin = SlimeLink.instance

		var attempts = 0
		val maxAttempts = 10
		var released = false
		fun releaseLocksOnce() {
			if (released) {
				return
			}
			released = true
			releaseWorldLocks(locks)
		}

		val unloadTask = object : BukkitRunnable() {
			override fun run() {
				if (Bukkit.isTickingWorlds()) {
					if (++attempts >= maxAttempts) {
						Skript.error("Failed to unload world '$worldName' after waiting for ticking to stop.")
						releaseLocksOnce()
						cancel()
					}
					return
				}

				cancel()
				try {
					var success: Boolean
					val time = measureTimeMillis {
						success = unloadWorld(bukkitWorld, !noSave)
					}

					if (success) {
						Skript.info("Successfully unloaded world '$worldName' in ${time}ms")
					} else {
						Skript.error("Failed to unload world '$worldName' in ${time}ms, it may still be loaded")
					}
				} finally {
					releaseLocksOnce()
				}
			}
		}

		try {
			unloadTask.runTaskTimer(plugin, 0L, 5L)
		} catch (throwable: Throwable) {
			releaseLocksOnce()
			throw throwable
		}
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
		val locks = acquireWorldLocks(listOf(worldName))
		val players = bukkitWorld.players
		if (players.isEmpty()) {
			unloadWorldSync(worldName, bukkitWorld, noSave, locks)
			return
		}

		try {
			require(shouldTeleport) {
				"Players in world '$worldName'; cannot unload without removing them"
			}

			val target = requireNotNull(teleportTarget) {
				"Teleport target location is null, unable to unload world '$worldName'"
			}

			teleportPlayersAndUnloadWorld(worldName, bukkitWorld, noSave, target, locks)
		} catch (throwable: Throwable) {
			releaseWorldLocks(locks)
			throw throwable
		}
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
		teleportTarget: Location,
		locks: List<String>? = null
	) {
		val playersInWorld = bukkitWorld.players
		val completableFuture =
			CompletableFuture.allOf(*playersInWorld.map { it.teleportAsync(teleportTarget) }.toTypedArray())

		completableFuture.thenRun {
			if (locks == null) {
				unloadWorldSync(worldName, bukkitWorld, noSave)
			} else {
				unloadWorldSync(worldName, bukkitWorld, noSave, locks)
			}
		}.exceptionally {
			if (locks != null) {
				releaseWorldLocks(locks)
			}
			Skript.error("Failed to teleport players and unload world '$worldName': ${it.message}")
			null
		}
	}

	private fun runAsyncLocked(
		operation: String,
		worldNames: Collection<String>,
		task: () -> Unit
	): CompletableFuture<Unit> {
		val locks = try {
			acquireWorldLocks(worldNames)
		} catch (throwable: Throwable) {
			return CompletableFuture.failedFuture(throwable)
		}

		val future = CompletableFuture<Unit>()
		val plugin = SlimeLink.instance

		try {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
				try {
					task()
					future.complete(Unit)
				} catch (throwable: Throwable) {
					logOperationFailure(operation, throwable)
					future.completeExceptionally(throwable)
				} finally {
					releaseWorldLocks(locks)
				}
			})
		} catch (throwable: Throwable) {
			releaseWorldLocks(locks)
			logOperationFailure(operation, throwable)
			future.completeExceptionally(throwable)
		}

		return future
	}

	private inline fun <T> withWorldLocks(
		worldNames: Collection<String>,
		block: () -> T
	): T {
		val locks = acquireWorldLocks(worldNames)
		return try {
			block()
		} finally {
			releaseWorldLocks(locks)
		}
	}

	private fun acquireWorldLocks(worldNames: Collection<String>): List<String> {
		val normalized = worldNames
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { it.lowercase(Locale.ROOT) }
            .distinct()
            .sorted()
            .toList()

		require(normalized.isNotEmpty()) { "No world names were provided for this operation." }

		val acquired = mutableListOf<String>()
		for (name in normalized) {
			if (!operationsInProgress.add(name)) {
				acquired.forEach(operationsInProgress::remove)
				throw IllegalStateException("Another operation is already running for world '$name'.")
			}
			acquired += name
		}
		return acquired
	}

	private fun releaseWorldLocks(locks: Collection<String>) {
		locks.forEach(operationsInProgress::remove)
	}

	fun clearOperationState() {
		operationsInProgress.clear()
	}

	private fun <T> runSyncAndWait(task: () -> T): T {
		if (Bukkit.isPrimaryThread()) {
			return task()
		}

		val latch = CountDownLatch(1)
		var result: T? = null
		var throwable: Throwable? = null

		val scheduledTask = Bukkit.getScheduler().runTask(SlimeLink.instance, Runnable {
			try {
				result = task()
			} catch (caught: Throwable) {
				throwable = caught
			} finally {
				latch.countDown()
			}
		})

		try {
			val completed = latch.await(SYNC_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			if (!completed) {
				scheduledTask.cancel()
				throw IllegalStateException(
					"Timed out after ${SYNC_OPERATION_TIMEOUT_SECONDS}s while waiting for a main-thread world operation."
				)
			}
		} catch (interrupted: InterruptedException) {
			Thread.currentThread().interrupt()
			throw IllegalStateException("Interrupted while waiting for a main-thread world operation.", interrupted)
		}

		throwable?.let { throw it }
		@Suppress("UNCHECKED_CAST")
		return result as T
	}

	private fun logOperationFailure(operation: String, throwable: Throwable) {
		val cause = unwrapException(throwable)
		val logger = SlimeLink.instance.slF4JLogger

		when (cause) {
			is WorldAlreadyExistsException,
			is UnknownWorldException,
			is CorruptedWorldException,
			is NewerFormatException,
			is InvalidWorldException,
			is WorldLoadedException,
			is WorldTooBigException,
			is IllegalArgumentException,
			is IllegalStateException -> logger.warn("Failed to {}: {}", operation, cause.message)

			else -> logger.error("Failed to {}.", operation, cause)
		}
	}

	private tailrec fun unwrapException(throwable: Throwable): Throwable {
		return when (throwable) {
			is ExecutionException,
			is java.util.concurrent.CompletionException -> {
				val cause = throwable.cause
				if (cause == null) throwable else unwrapException(cause)
			}

			else -> throwable
		}
	}
}
