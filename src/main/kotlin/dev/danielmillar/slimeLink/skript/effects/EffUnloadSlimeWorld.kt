package dev.danielmillar.slimeLink.skript.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import dev.danielmillar.slimeLink.SlimeLink
import dev.danielmillar.slimeLink.config.ConfigManager
import dev.danielmillar.slimeLink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimeLink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.event.Event
import java.util.concurrent.CompletableFuture

@Name("Unload Slime World")
@Description("Unload a Slime World with a specified name.")
@Examples(
    value = [
        "unload slime world named \"Test\" with type %file%"
    ]
)
@Since("1.0.0")
class EffUnloadSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUnloadSlimeWorld::class.java,
                "unload (slimeworld|slime world) named %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Unload slime world ${worldName.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>

        return true
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
            val success = Bukkit.unloadWorld(bukkitWorld, true)
            if (success) {
                Skript.info("World $worldNameValue unloaded successfully")
            } else {
                Skript.error("World $worldNameValue failed to unload")
            }
            return
        }

        val defaultWorld = Bukkit.getWorlds().firstOrNull() ?: run {
            Skript.error("No default world found to teleport players to")
            return
        }
        val spawnLocation = defaultWorld.spawnLocation

        val futures = playersInWorld.map { it.teleportAsync(spawnLocation) }
        val allOfFuture = CompletableFuture.allOf(*futures.toTypedArray())

        allOfFuture.thenRun {
            val success = if (worldData.isReadOnly()) {
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

            if (success) {
                Skript.info("World $worldNameValue unloaded successfully")
            } else {
                Skript.error("World $worldNameValue failed to unload")
            }
        }
    }
}
