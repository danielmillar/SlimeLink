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

@Name("SlimeWorld Exists in Config")
@Description("Checks if a SlimeWorld with the specified name exists in the configuration.")
@Examples(
    value = [
        "if slime world named \"myWorld\" exists:",
        "\t# Do something if the world exists",
        "if slime world named \"myWorld\" does not exist:",
        "\t# Do something if the world does not exist"
    ]
)
@Since("1.0.0")
class CondSlimeWorldExistsConfig : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondSlimeWorldExistsConfig::class.java,
                "(slimeworld|slime world) named %string% (1¦exists|2¦does(n't| not))"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "${worldName.toString(event, debug)} ${if (isNegated) "does not exist" else "exists"}"
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
        return if (isNegated) {
            worldData == null
        } else {
            worldData != null
        }
    }
}
