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

@Name("SlimeWorld Exists")
@Description("Check if a SlimeWorld already exists in the config.")
@Examples(
    value = [
        "if slime world named \"MySlimeWorld\" exists:",
        "if slime world named \"MySlimeWorld\" doesn't exist:"
    ]
)
@Since("1.1.2")
class CondSlimeWorldExists : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondSlimeWorldExists::class.java,
                "(slimeworld|slime world) named %string% (exists|1:doesn't exist)"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "slime world named ${worldName.toString(event, debug)} ${if (isNegated) "doesn't exist" else "exists"}"
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

        val exists = worldData != null
        return if (isNegated) !exists else exists
    }
}
