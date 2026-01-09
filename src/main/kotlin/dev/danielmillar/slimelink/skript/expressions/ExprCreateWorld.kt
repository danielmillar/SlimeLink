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
import com.infernalsuite.asp.api.loaders.SlimeLoader
import com.infernalsuite.asp.api.world.SlimeWorld
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.util.SlimeWorldUtils.createWorldSync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Create World")
@Description(
    "Creates a new empty SlimeWorld and returns the SlimeWorld object.",
    "The created world is not saved to its loader or loaded into Bukkit; it exists only in memory.",
    "Use save or load effects when you need persistence or to make the world on the server."
)
@Examples(
    value = [
        "set {_world} to create slime world named \"MyWorld\" with {_loader}",
    ]
)
@Since("2.0.0")
class ExprCreateWorld : SimpleExpression<SlimeWorld>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprCreateWorld::class.java,
                SlimeWorld::class.java,
                ExpressionType.SIMPLE,
                "(create|new) (slimeworld|slime world) named %string% [readonly:as readonly] with %slimeloader% [using %-slimepropertymap%]"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>
    private var properties: Expression<SlimePropertyMap>? = null
    private var readOnly = false

    override fun toString(event: Event?, debug: Boolean): String =
        "create slime world ${worldName.toString(event, debug)} ${if (readOnly) "as readonly " else ""}with ${loader.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        loader = expressions[1] as Expression<SlimeLoader>
        properties = expressions[2] as? Expression<SlimePropertyMap>
        readOnly = parser.hasTag("readonly")
        return true
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<SlimeWorld> = SlimeWorld::class.java

    override fun get(event: Event): Array<SlimeWorld?> {
        val name = worldName.getSingle(event) ?: return emptyArray()
        val slimeLoader = loader.getSingle(event) ?: return emptyArray()
        val props = properties?.getSingle(event) ?: SlimePropertyMap()

        return try {
            validateWorldName(name)
            requireWorldNotLoaded(name, "A loaded world with that name already exists!")
            val world = createWorldSync(name, props, slimeLoader, readOnly)
            arrayOf(world)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
            emptyArray()
        }
    }
}
