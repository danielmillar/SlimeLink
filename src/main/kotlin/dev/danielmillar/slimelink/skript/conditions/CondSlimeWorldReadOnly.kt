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

@Name("Check Slime World ReadOnly")
@Description("Checks if the specified world is readonly or not.")
@Examples(
    value = [
        "if slime world named \"exampleWorld\" is readonly:",
        "    broadcast \"World is read-only!\"",
        "if slimeworld named \"testWorld\" isn't readonly:",
        "    broadcast \"World can be modified!\"",
        "if slime world named \"anotherWorld\" is not readonly:",
        "    broadcast \"World can be modified!\""
    ]
)
@Since("1.0.0")
class CondSlimeWorldReadOnly : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondSlimeWorldReadOnly::class.java,
                "(slimeworld|slime world) named %string% (1¦is|2¦is(n't| not)) readonly"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "${worldName.toString(event, debug)} ${if (isNegated) "isn't" else "is"} readonly"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        isNegated = parseResult?.mark == 2
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

        return if (isNegated) {
            !worldData.isReadOnly()
        } else {
            worldData.isReadOnly()
        }
    }
}
