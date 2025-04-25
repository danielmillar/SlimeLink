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
import dev.danielmillar.slimelink.util.SlimeWorldUtils
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.Event

@Name("Unload Slime World By Object")
@Description("Unload a Slime World using a Bukkit World object. Optionally teleport players out of the world, either to the default world spawn or to a specified location.")
@Examples(
    value = [
        "unload slimeworld {world}",
        "unload slime world {myWorld}",
        "unload slimeworld {world} and teleport",
        "unload slime world {myWorld} and teleport",
        "unload slimeworld {world} and teleport to {spawnLocation}",
        "unload slime world {myWorld} and teleport to location(0, 64, 0, world(\"world\"))"
    ]
)
@Since("1.0.0")
class EffUnloadSlimeWorldByObject : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUnloadSlimeWorldByObject::class.java,
                "unload (slimeworld|slime world) %world% [teleport:and teleport [to %-location%]]"
            )
        }
    }

    private lateinit var world: Expression<World>
    private var shouldTeleport = false
    private var teleportLocation: Expression<Location>? = null

    override fun toString(event: Event?, debug: Boolean): String {
        val locationStr = teleportLocation?.let { " to ${it.toString(event, debug)}" } ?: ""
        return "Unload slime world ${world.toString(event, debug)}${if (shouldTeleport) " and teleport$locationStr" else ""}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        world = expressions[0] as Expression<World>
        shouldTeleport = parseResult?.hasTag("teleport") ?: false
        if (expressions.size > 1 && expressions[1] != null) {
            teleportLocation = expressions[1] as Expression<Location>
        }
        return true
    }

    override fun execute(event: Event) {
        val bukkitWorld = world.getSingle(event) ?: return
        val worldNameValue = bukkitWorld.name

        // Get world data
        val worldData = SlimeWorldUtils.getWorldData(worldNameValue) ?: return

        val playersInWorld = bukkitWorld.players
        if (playersInWorld.isEmpty()) {
            // No players in world, unload directly
            val success = SlimeWorldUtils.unloadWorld(worldNameValue, bukkitWorld, worldData)
            SlimeWorldUtils.handleUnloadResult(worldNameValue, success)
            return
        }

        if (!shouldTeleport) {
            // Players in world but no teleport requested
            val success = SlimeWorldUtils.unloadWorld(worldNameValue, bukkitWorld, worldData)
            SlimeWorldUtils.handleUnloadResult(worldNameValue, success)
            return
        }

        // Get teleport target location
        val customLocation = teleportLocation?.getSingle(event)
        val teleportTarget = SlimeWorldUtils.getTeleportTarget(customLocation) ?: return

        // Teleport players and unload world
        SlimeWorldUtils.teleportPlayersAndUnloadWorld(worldNameValue, bukkitWorld, worldData, teleportTarget)
    }
}
