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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.unloadWithOptionalTeleport
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.Location
import org.bukkit.event.Event

@Name("SlimeWorld - Unload World")
@Description(
    "Unloads a loaded SlimeWorld from the server",
)
@Examples(
    value = [
        "unload slime world \"MyWorld\"",
        "unload slime world \"MyWorld\" without saving",
        "unload slime world \"MyWorld\" teleporting players to spawn of world \"world\""
    ]
)
@Since("2.0.0")
class EffUnloadWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUnloadWorld::class.java,
                "unload slime world %string% [nosave:without saving] [teleporting players to %-location%]"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private var teleportLocation: Expression<Location>? = null
    private var noSave = false

    override fun toString(event: Event?, debug: Boolean): String =
        "unload slime world ${worldName.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        teleportLocation = expressions[1] as? Expression<Location>
        noSave = parseResult?.hasTag("nosave") ?: false
        return true
    }

    override fun execute(event: Event) {
        val name = worldName.getSingle(event) ?: return
        val teleportTarget = teleportLocation?.getSingle(event)

        try {
            validateWorldName(name)
            val world = requireWorldLoaded(name)
            unloadWithOptionalTeleport(
                name,
                world,
                noSave,
                teleportTarget != null,
                teleportTarget
            )
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
