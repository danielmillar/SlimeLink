package dev.danielmillar.slimelink.skript.expressions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.Event

@Name("SlimeWorld - Fetch World")
@Description(
    "Returns the Bukkit World for a loaded SlimeWorld by name.",
    "The world must be loaded on the server, an empty result is returned if not found or not loaded.",
)
@Examples(
    value = [
        "set {_world} to slime world named \"MyWorld\"",
        "teleport player to spawn of (slime world named \"lobby\")"
    ]
)
@Since("1.0.0")
class ExprFetchWorld : SimpleExpression<World>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprFetchWorld::class.java,
                World::class.java,
                ExpressionType.COMBINED,
                "fetch (slimeworld|slime world) named %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String =
        "slime world named ${worldName.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        return true
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<World> = World::class.java

    override fun get(event: Event): Array<World> {
        val name = worldName.getSingle(event) ?: return emptyArray()

        return try {
            validateWorldName(name)
            val world = Bukkit.getWorld(name) ?: return emptyArray()
            arrayOf(world)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
            emptyArray()
        }
    }
}
