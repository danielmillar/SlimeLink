package dev.danielmillar.slimelink.skript.conditions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import dev.danielmillar.slimelink.config.ConfigManager
import org.bukkit.event.Event
import kotlin.jvm.java

@Name("SlimeWorld ReadOnly")
@Description("Check if a SlimeWorld is set to read-only")
@Examples(
    value = [
        "if slime world named \"MySlimeWorld\" is readonly:",
        "if slime world named \"MySlimeWorld\" isn't readonly:",
        "if slime world named \"MySlimeWorld\" is not readonly:",
    ]
)
@Since("1.0.0")
class CondSlimeWorldReadOnly : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondSlimeWorldReadOnly::class.java,
                "(slimeworld|slime world) named %string% (is|1:is(n't| not)) (readonly|read-only)"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "slime world named ${worldName.toString(event, debug)} ${if (isNegated) "isn't read-only" else "is read-only"}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        isNegated = parseResult.hasTag("1")
        return true
    }

    override fun check(event: Event?): Boolean {
        if (event == null) return false

        val worldNameValue = worldName.getSingle(event) ?: return false

        val worldData = ConfigManager.getWorldConfig().getWorld(worldNameValue)
        if (worldData == null) {
            Skript.error("World $worldNameValue cannot be found in config")
            return false
        }

        val isReadOnly = worldData.isReadOnly()
        return if (isNegated) !isReadOnly else isReadOnly
    }
}
