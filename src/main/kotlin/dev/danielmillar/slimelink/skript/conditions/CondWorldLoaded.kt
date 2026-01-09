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
import org.bukkit.Bukkit
import org.bukkit.event.Event

@Name("SlimeWorld - Is Loaded")
@Description(
    "This condition checks if a SlimeWorld is loaded or not",
    "Internally this just checks if a Bukkit world can be found.")
@Examples(
    value = [
        "if slimeworld named \"MySlimeWorld\" is loaded:",
        "if slimeworld named \"MySlimeWorld\" isn't loaded:",
        "if slime world named \"MySlimeWorld\" is loaded:",
        "if slime world named \"MySlimeWorld\" is not loaded:",
    ]
)
@Since("1.0.0")
class CondWorldLoaded : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondWorldLoaded::class.java,
                "(slimeworld|slime world) named %string% (is|1:is(n't| not)) loaded"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions.getOrNull(0) as? Expression<String> ?: return false
        isNegated = parser.hasTag("1")

        return true
    }

    override fun check(event: Event): Boolean {
        val name = worldName.getSingle(event) ?: return false
        val worldExists = Bukkit.getWorld(name) != null
        return worldExists != isNegated
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "${worldName.toString(event, debug)} ${if (isNegated) "isn't" else "is"} loaded"
    }
}
