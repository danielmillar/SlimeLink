package dev.danielmillar.slimelink.skript.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.config.WorldData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.Event
import java.util.concurrent.CompletableFuture

@Name("Unload Slime World")
@Description("Unload a Slime World with a specified name. Optionally teleport players out of the world, either to the default world spawn or to a specified location.")
@Examples(
    value = [
        "unload slimeworld named \"Test\"",
        "unload slime world named \"MyWorld\"",
        "unload slimeworld named \"Test\" and teleport",
        "unload slime world named \"MyWorld\" and teleport",
        "unload slimeworld named \"Test\" and teleport to {spawnLocation}",
        "unload slime world named \"MyWorld\" and teleport to location(0, 64, 0, world(\"world\"))"
    ]
)
@Since("1.0.0")
class EffUnloadSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUnloadSlimeWorld::class.java,
                "unload (slimeworld|slime world) named %string% [teleport:and teleport [to %-location%]]"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private var shouldTeleport = false
    private var teleportLocation: Expression<Location>? = null

    override fun toString(event: Event?, debug: Boolean): String {
        val locationStr = teleportLocation?.let { " to ${it.toString(event, debug)}" } ?: ""
        return "Unload slime world ${worldName.toString(event, debug)}${if (shouldTeleport) " and teleport$locationStr" else ""}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        shouldTeleport = parseResult?.hasTag("teleport") ?: false
        if (expressions.size > 1 && expressions[1] != null) {
            teleportLocation = expressions[1] as Expression<Location>
        }
        return true
    }

    private fun unloadWorld(worldNameValue: String, bukkitWorld: World, worldData: WorldData): Boolean {
        return if (worldData.isReadOnly()) {
            Bukkit.unloadWorld(bukkitWorld, false)
        } else {
            try {
                val loadedWorld = SlimeLink.getASP().getLoadedWorld(worldNameValue)
                SlimeLink.getASP().saveWorld(loadedWorld)

                ConfigManager.getWorldConfig().setWorld(worldNameValue, worldData)
                ConfigManager.saveWorldConfig()

                Bukkit.unloadWorld(bukkitWorld, true)
            } catch (ex: Exception) {
                Skript.error("Failed to save world $worldNameValue before unloading: ${ex.message}")
                ex.printStackTrace()
                false
            }
        }
    }

    private fun handleUnloadResult(worldNameValue: String, success: Boolean) {
        if (success) {
            Skript.info("World $worldNameValue unloaded successfully")
        } else {
            Skript.error("World $worldNameValue failed to unload")
        }
    }

    override fun execute(event: Event) {
        val worldNameValue = worldName.getSingle(event) ?: return

        val bukkitWorld = Bukkit.getWorld(worldNameValue)
        if (bukkitWorld == null) {
            Skript.error("World $worldNameValue isn't loaded, can't unload!")
            return
        }

        val worldData = ConfigManager.getWorldConfig().getWorld(worldNameValue)
        if (worldData == null) {
            Skript.error("World $worldNameValue cannot be found in config")
            return
        }

        val playersInWorld = bukkitWorld.players
        if (playersInWorld.isEmpty()) {
            val success = unloadWorld(worldNameValue, bukkitWorld, worldData)
            handleUnloadResult(worldNameValue, success)
            return
        }

        if (!shouldTeleport) {
            val success = unloadWorld(worldNameValue, bukkitWorld, worldData)
            handleUnloadResult(worldNameValue, success)
            return
        }

        val customLocation = teleportLocation?.getSingle(event)
        val teleportTarget = if (customLocation != null) {
            customLocation
        } else {
            val defaultWorld = Bukkit.getWorlds().firstOrNull() ?: run {
                Skript.error("No default world found to teleport players to")
                return
            }
            defaultWorld.spawnLocation
        }

        val futures = playersInWorld.map { it.teleportAsync(teleportTarget) }
        val allOfFuture = CompletableFuture.allOf(*futures.toTypedArray())

        allOfFuture.thenRun {
            val success = unloadWorld(worldNameValue, bukkitWorld, worldData)
            handleUnloadResult(worldNameValue, success)
        }
    }
}
