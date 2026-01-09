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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.saveWorldSync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Save World")
@Description(
    "Saves the current state of a loaded SlimeWorld to its configured loader.",
)
@Examples(
    value = [
        "save slime world \"MyWorld\"",
        "save slime world {_worldName}"
    ]
)
@Since("2.0.0")
class EffSaveWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffSaveWorld::class.java,
                "save slime world %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String =
        "save slime world ${worldName.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
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
        val name = worldName.getSingle(event) ?: return

        try {
            validateWorldName(name)
            requireWorldLoaded(name)
            saveWorldSync(name)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
