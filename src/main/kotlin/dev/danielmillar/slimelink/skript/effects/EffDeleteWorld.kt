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
import com.infernalsuite.asp.api.loaders.SlimeLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.deleteWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Delete World")
@Description(
    "Deletes a Slime world from the specified loader, removing it from storage permanently."
)
@Examples(
    value = [
        "delete slime world \"MyWorld\" from {_loader}",
    ]
)
@Since("2.0.0")
class EffDeleteWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffDeleteWorld::class.java,
                "delete slime world %string% from %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>

    override fun toString(event: Event?, debug: Boolean): String {
        return "delete slime world ${worldName.toString(event, debug)} from ${loader.toString(event, debug)}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        loader = expressions[1] as Expression<SlimeLoader>
        return true
    }

    override fun execute(event: Event) {
        val name = worldName.getSingle(event) ?: return
        val slimeLoader = loader.getSingle(event) ?: return

        try {
            validateWorldName(name)
            requireWorldNotLoaded(name, "World is currently loaded, must be unloaded before deleting!")
            deleteWorldAsync(name, slimeLoader)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
