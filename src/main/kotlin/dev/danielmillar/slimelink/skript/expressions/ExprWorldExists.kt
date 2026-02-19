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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - World Exists")
@Description(
    "Returns whether a SlimeWorld exists in the specified SlimeLoader.",
)
@Examples(
    value = [
        "set {_exists} to whether slime world named \"lobby\" exists in {_loader}",
        "if whether slime world named \"arena\" exists in {_loader}:"
    ]
)
@Since("2.0.0")
class ExprWorldExists : SimpleExpression<Boolean>() {

    companion object {
        init {
            registerSimpleExpression(
                ExprWorldExists::class.java,
                Boolean::class.javaObjectType,
                "[whether] (slimeworld|slime world) named %string% (0:exists|1:does(n't| not) exist) (in|from) %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>
    private var expectExists = true

    override fun toString(event: Event?, debug: Boolean): String {
        val state = if (expectExists) "exists" else "does not exist"
        return "whether slime world named ${worldName.toString(event, debug)} $state in ${loader.toString(event, debug)}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions[0] as? Expression<String> ?: return false
        loader = expressions[1] as? Expression<SlimeLoader> ?: return false
        expectExists = parser.mark == 0
        return true
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<Boolean> = Boolean::class.javaObjectType

    override fun get(event: Event): Array<Boolean> {
        val name = worldName.getSingle(event) ?: return emptyArray()
        val slimeLoader = loader.getSingle(event) ?: return emptyArray()

        return try {
            validateWorldName(name)
            arrayOf(slimeLoader.worldExists(name) == expectExists)
        } catch (exception: Exception) {
            this.error(userFacingError(exception))
            emptyArray()
        }
    }
}
