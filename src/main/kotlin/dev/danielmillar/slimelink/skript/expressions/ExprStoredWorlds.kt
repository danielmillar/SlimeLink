package dev.danielmillar.slimelink.skript.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.loaders.SlimeLoader
import dev.danielmillar.slimelink.skript.registerSimpleExpression
import dev.danielmillar.slimelink.util.SlimeWorldUtils.userFacingError
import org.bukkit.event.Event

@Name("SlimeWorld - Stored Worlds")
@Description(
    "Returns all world names currently stored in the specified SlimeLoader.",
)
@Examples(
    value = [
        "set {_stored::*} to all stored slime worlds in {_loader}",
        "loop all stored slime world names from {_loader}:",
        "    broadcast loop-value"
    ]
)
@Since("2.0.0")
class ExprStoredWorlds : SimpleExpression<String>() {

    companion object {
        init {
            registerSimpleExpression(
                ExprStoredWorlds::class.java,
                String::class.java,
                "all stored (slimeworlds|slime worlds) (in|from) %slimeloader%",
                "all stored (slimeworld|slime world) names (in|from) %slimeloader%"
            )
        }
    }

    private lateinit var loader: Expression<SlimeLoader>

    override fun toString(event: Event?, debug: Boolean): String =
        "all stored slime worlds in ${loader.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult?
    ): Boolean {
        loader = expressions[0] as? Expression<SlimeLoader> ?: return false
        return true
    }

    override fun isSingle(): Boolean = false

    override fun getReturnType(): Class<String> = String::class.java

    override fun get(event: Event): Array<String> {
        val slimeLoader = loader.getSingle(event) ?: return emptyArray()
        return try {
            slimeLoader.listWorlds().toTypedArray()
        } catch (exception: Exception) {
            this.error(userFacingError(exception))
            emptyArray()
        }
    }
}
