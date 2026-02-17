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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.cloneWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Clone World")
@Description(
    "This effect clones a SlimeWorld to another with the provided loader",
    "You can specify if you want the clone to be readonly or pass in a new set of properties"
)
@Examples(
    value = [
        "clone slime world \"Test\" to \"TestCopy\" with {_loader}",
        "clone slime world \"Test\" to \"TestCopy\" with {_loader} using {_properties}",
        "clone slime world \"Test\" to \"TestCopy\" as readonly with {_loader} using {_properties}",
    ]
)
@Since("2.0.0")
class EffCloneWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffCloneWorld::class.java,
                "clone (slimeworld|slime world) %string% to %string% [readonly:as readonly] with %slimeloader% [using %-slimepropertymap%]"
            )
        }
    }

    private lateinit var sourceWorldName: Expression<String>
    private lateinit var targetWorldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>
    private var properties: Expression<SlimePropertyMap>? = null
    private var readOnly = false

    override fun toString(event: Event?, debug: Boolean): String =
        "clone slime world ${sourceWorldName.toString(event, debug)} to ${targetWorldName.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        sourceWorldName = expressions[0] as Expression<String>
        targetWorldName = expressions[1] as Expression<String>
        loader = expressions[2] as Expression<SlimeLoader>
        properties = expressions[3] as? Expression<SlimePropertyMap>
        readOnly = parseResult?.hasTag("readonly") ?: false
        return true
    }

    override fun execute(event: Event) {
        val sourceName = sourceWorldName.getSingle(event) ?: return
        val targetName = targetWorldName.getSingle(event) ?: return
        val slimeLoader = loader.getSingle(event) ?: return
        val props = properties?.getSingle(event) ?: SlimePropertyMap()

        try {
            validateWorldName(sourceName)
            validateWorldName(targetName)
            requireWorldNotLoaded(targetName)
            
            require(!sourceName.equals(targetName, ignoreCase = true)) {
                "Source and target world names must be different"
            }
            
            cloneWorldAsync(sourceName, targetName, slimeLoader, readOnly, props)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
