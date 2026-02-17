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
import org.bukkit.Bukkit
import org.bukkit.event.Event

@Name("SlimeWorld - Loaded Worlds")
@Description(
    "Returns the names of all SlimeWorlds from the specified loader that are currently loaded on the server.",
)
@Examples(
    value = [
        "set {_worlds::*} to all loaded slime worlds with {_loader}",
        "loop all loaded slime worlds with {_loader}:",
        "    broadcast loop-value"
    ]
)
@Since("1.0.0")
class ExprLoadedWorlds : SimpleExpression<String>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprLoadedWorlds::class.java,
                String::class.java,
                ExpressionType.SIMPLE,
                "all loaded (slimeworlds|slime worlds) with %slimeloader%"
            )
        }
    }

    private lateinit var loader: Expression<SlimeLoader>

    override fun toString(event: Event?, debug: Boolean): String =
        "all loaded slime worlds with ${loader.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult?
    ): Boolean {
        loader = expressions[0] as Expression<SlimeLoader>
        return true
    }

    override fun isSingle(): Boolean = false

    override fun getReturnType(): Class<String> = String::class.java

    override fun get(event: Event): Array<String> {
        val slimeLoader = loader.getSingle(event) ?: return emptyArray()

        return try {
            val loaderWorlds = slimeLoader.listWorlds()
            val loadedWorldNames = Bukkit.getWorlds().map { it.name }
            loaderWorlds.filter { it in loadedWorldNames }.toTypedArray()
        } catch (e: Exception) {
            Skript.error("Failed to list worlds: ${e.message}")
            emptyArray()
        }
    }
}
