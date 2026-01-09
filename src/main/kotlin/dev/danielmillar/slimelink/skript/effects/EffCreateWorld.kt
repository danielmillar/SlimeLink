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
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.util.SlimeWorldUtils.createWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Create World")
@Description(
    "Creates a new empty SlimeWorld and saves it to the specified loader.",
    "You are required to load the world manually afterwards"
)
@Examples(
    value = [
        "create slime world named \"MyWorld\" with {_loader}",
        "load slime world \"MyWorld\" with {_loader}"
    ]
)
@Since("2.0.0")
class EffCreateWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffCreateWorld::class.java,
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
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        loader = expressions[1] as Expression<SlimeLoader>
        properties = expressions[2] as? Expression<SlimePropertyMap>
        readOnly = parseResult.hasTag("readonly")
        return true
    }

    override fun execute(event: Event) {
        val name = worldName.getSingle(event) ?: return
        val slimeLoader = loader.getSingle(event) ?: return
        val props = properties?.getSingle(event) ?: SlimePropertyMap()

        try {
            validateWorldName(name)
            requireWorldNotLoaded(name, "A loaded world with that name already exists!")
            createWorldAsync(name, props, slimeLoader, readOnly)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
