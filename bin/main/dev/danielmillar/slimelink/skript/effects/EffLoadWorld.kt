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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.loadWorldSync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Load World")
@Description(
    "Loads an existing SlimeWorld from the specified loader onto the server",
)
@Examples(
    value = [
        "load slime world \"MyWorld\" with {_loader}",
        "load slime world \"MyWorld\" with {_loader} as readonly",
        "load slime world \"MyWorld\" with {_loader} using {_properties}"
    ]
)
@Since("2.0.0")
class EffLoadWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffLoadWorld::class.java,
                "load slime world %string% with %slimeloader% [readonly:as readonly] [using %-slimepropertymap%]"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>
    private var properties: Expression<SlimePropertyMap>? = null
    private var readOnly = false

    override fun toString(event: Event?, debug: Boolean): String {
        return "load slime world ${worldName.toString(event, debug)} with ${loader.toString(event, debug)}"
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
        properties = expressions[2] as? Expression<SlimePropertyMap>
        readOnly = parseResult?.hasTag("readonly") ?: false
        return true
    }

    override fun execute(event: Event) {
        val name = worldName.getSingle(event) ?: return
        val slimeLoader = loader.getSingle(event) ?: return
        val props = properties?.getSingle(event) ?: SlimePropertyMap()

        try {
            validateWorldName(name)
            requireWorldNotLoaded(name)
            loadWorldSync(name, slimeLoader, readOnly, props)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
