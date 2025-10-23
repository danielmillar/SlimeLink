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
import kotlin.jvm.java

@Name("SlimeWorld Loaded")
@Description("Check if a SlimeWorld is currently loaded on the server.")
@Examples(
    value = [
        "if slime world named \"MySlimeWorld\" is loaded:",
        "if slime world named \"MySlimeWorld\" isn't loaded:",
    ]
)
@Since("1.0.0")
class CondSlimeWorldLoaded : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondSlimeWorldLoaded::class.java,
                "(slimeworld|slime world) named %string% (is|1:is(n't| not)) loaded"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "${worldName.toString(event, debug)} ${if (isNegated) "isn't" else "is"} loaded"
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
        val bukkitWorld = Bukkit.getWorld(worldNameValue)

        val isLoaded = bukkitWorld != null
        return if (isNegated) !isLoaded else isLoaded
    }
}
