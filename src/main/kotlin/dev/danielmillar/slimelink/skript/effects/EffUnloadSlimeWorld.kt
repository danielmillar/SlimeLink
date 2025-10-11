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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.unloadWithOptionalTeleport
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.Location
import org.bukkit.event.Event

@Name("Unload Slime World")
@Description(
    value = [
        "This effect allows you to unload a specific SlimeWorld.",
        "There is an optional to teleport players out of the world as there must be no players in the world while unloading."
    ]
)
@Examples(
    value = [
        "unload slimeworld named \"MyWorld\"",
        "unload slime world named \"MyWorld\"",
        "unload slimeworld named \"MyWorld\" and teleport to {spawnLocation}",
        "unload slime world named \"MyWorld\" and teleport to location(0, 64, 0, world(\"world\"))"
    ]
)
@Since("1.0.0")
class EffUnloadSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUnloadSlimeWorld::class.java,
                "unload (slimeworld|slime world) named %string% [teleport:and teleport to %-location%]"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private var shouldTeleport = false
    private var teleportLocation: Expression<Location>? = null

    override fun toString(event: Event?, debug: Boolean): String {
        val locationStr = teleportLocation?.let { " to ${it.toString(event, debug)}" } ?: ""
        return "Unload slime world ${
            worldName.toString(
                event,
                debug
            )
        }${if (shouldTeleport) " and teleport$locationStr" else ""}"
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

    override fun execute(event: Event) {
        val worldNameValue = worldName.getSingle(event) ?: return

        try {
            validateWorldName(worldNameValue)

            val world = requireWorldLoaded(worldNameValue)
            val worldData = requireWorldDataExists(worldNameValue)

            unloadWithOptionalTeleport(worldNameValue, world, worldData, shouldTeleport, teleportLocation?.getSingle(event))
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
