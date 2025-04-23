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
import dev.danielmillar.slimelink.config.ConfigManager
import org.bukkit.event.Event

@Name("Fetch All Slime Worlds")
@Description("List all SlimeWorlds from the config file.")
@Examples(
    value = [
        "set {allSlimeWorlds::*} to all slime worlds"
    ]
)
@Since("1.0.0")
class ExprFetchAllSlimeWorlds : SimpleExpression<String>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprFetchAllSlimeWorlds::class.java,
                String::class.java,
                ExpressionType.SIMPLE,
                "all (slimeworlds|slime worlds)"
            )
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "Fetching all slime worlds from config"
    }

    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult?
    ): Boolean {
        return true
    }

    override fun isSingle(): Boolean {
        return false
    }

    override fun getReturnType(): Class<String> {
        return String::class.java
    }

    override fun get(event: Event): Array<String> {
        return ConfigManager.getWorldConfig().getWorlds().keys.toTypedArray()
    }
}