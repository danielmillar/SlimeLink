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
import com.infernalsuite.asp.api.world.SlimeWorld
import dev.danielmillar.slimelink.util.SlimeWorldUtils.loadWorldSync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import org.bukkit.event.Event

@Name("SlimeWorld - Load World")
@Description(
    "Loads a SlimeWorld object onto the server"
)
@Examples(
    value = [
        "load slime world {_world}"
    ]
)
@Since("2.0.0")
class EffLoadWorldObject : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffLoadWorldObject::class.java,
                "load slime world %slimeworld%"
            )
        }
    }

    private lateinit var world: Expression<SlimeWorld>

    override fun toString(event: Event?, debug: Boolean): String {
        return "load slime world ${world.getSingle(event)?.name}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        world = expressions.getOrNull(0) as Expression<SlimeWorld>
        return true
    }

    override fun execute(event: Event) {
        val world = world.getSingle(event) ?: return

        try {
            requireWorldNotLoaded(world.name)
            loadWorldSync(world)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
