package dev.danielmillar.slimelink.skript.conditions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.loaders.SlimeLoader
import dev.danielmillar.slimelink.skript.registerCondition
import dev.danielmillar.slimelink.util.SlimeWorldUtils.userFacingError
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Exists In Loader")
@Description(
    "Checks whether a SlimeWorld is stored in the provided SlimeLoader.",
)
@Examples(
    value = [
        "if slime world named \"lobby\" exists in {_loader}:",
        "if slime world named \"match_01\" does not exist in {_loader}:"
    ]
)
@Since("2.0.0")
class CondWorldExists : Condition() {

    companion object {
        init {
            registerCondition(
                CondWorldExists::class.java,
                "(slimeworld|slime world) named %string% (0:exists|1:does(n't| not) exist) (in|from) %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions[0] as? Expression<String> ?: return false
        loader = expressions[1] as? Expression<SlimeLoader> ?: return false
        isNegated = parser.mark == 1
        return true
    }

    override fun check(event: Event): Boolean {
        val name = worldName.getSingle(event) ?: return false
        val slimeLoader = loader.getSingle(event) ?: return false

        return try {
            validateWorldName(name)
            val exists = slimeLoader.worldExists(name)
            exists != isNegated
        } catch (exception: Exception) {
            this.error(userFacingError(exception))
            false
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val state = if (isNegated) "does not exist" else "exists"
        return "slime world named ${worldName.toString(event, debug)} $state in ${loader.toString(event, debug)}"
    }
}
