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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.cloneWorldSync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Clone World")
@Description(
    "Clones an existing Slime World and returns the cloned SlimeWorld object.",
    "The clone is saved to the loader by default.",
    "Use the 'not stored' option to prevent saving, the cloned world is also not loaded automatically."
)
@Examples(
    value = [
        "set {_world} to clone slime world \"Test\" to \"TestCopy\" with {_loader}",
        "set {_world} to clone slime world \"Test\" to \"TestCopy\" not stored with {_loader}",
    ]
)
@Since("2.0.0")
class ExprCloneWorld : SimpleExpression<SlimeWorld>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprCloneWorld::class.java,
                SlimeWorld::class.java,
                ExpressionType.SIMPLE,
                "clone (slimeworld|slime world) %string% to %string% [readonly:as readonly] [nostore:not stor(ed|ing)] [with %-slimeloader%] [using %-slimepropertymap%]"
            )
        }
    }

    private lateinit var sourceWorldName: Expression<String>
    private lateinit var targetWorldName: Expression<String>
    private var loader: Expression<SlimeLoader>? = null
    private var properties: Expression<SlimePropertyMap>? = null
    private var readOnly = false
    private var noStore = false

    override fun toString(event: Event?, debug: Boolean): String =
        "clone slime world ${sourceWorldName.toString(event, debug)} to ${targetWorldName.toString(event, debug)} ${if (readOnly) "as readonly " else ""}with ${loader?.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean {
        sourceWorldName = expressions[0] as Expression<String>
        targetWorldName = expressions[1] as Expression<String>
        loader = expressions[2] as? Expression<SlimeLoader>
        properties = expressions[3] as? Expression<SlimePropertyMap>
        readOnly = parser.hasTag("readonly")
        noStore = parser.hasTag("nostore")
        return true
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<SlimeWorld> = SlimeWorld::class.java

    override fun get(event: Event): Array<SlimeWorld?> {
        val sourceName = sourceWorldName.getSingle(event) ?: return emptyArray()
        val targetName = targetWorldName.getSingle(event) ?: return emptyArray()
        val slimeLoader = loader?.getSingle(event)
        val props = properties?.getSingle(event) ?: SlimePropertyMap()

        return try {
            validateWorldName(sourceName)
            validateWorldName(targetName)
            requireWorldNotLoaded(targetName)
            
            require(!sourceName.equals(targetName, ignoreCase = true)) {
                "Source and target world names must be different"
            }
            
            val world = cloneWorldSync(sourceName, targetName, slimeLoader, readOnly, props, !noStore)
            arrayOf(world)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
            emptyArray()
        }
    }
}
