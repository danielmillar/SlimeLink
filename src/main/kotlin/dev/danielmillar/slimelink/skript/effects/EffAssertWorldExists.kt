package dev.danielmillar.slimelink.skript.effects

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.loaders.SlimeLoader
import dev.danielmillar.slimelink.skript.registerEffect
import dev.danielmillar.slimelink.util.SlimeWorldUtils.userFacingError
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event

@Name("SlimeWorld - Assert World Exists")
@Description(
    "Asserts whether a SlimeWorld exists in a SlimeLoader.",
    "If the assertion fails, a Skript runtime error is reported."
)
@Examples(
    value = [
        "assert slime world named \"lobby\" exists in {_loader}",
        "ensure slime world named \"temp_world\" does not exist in {_loader}"
    ]
)
@Since("2.0.0")
class EffAssertWorldExists : Effect() {

    companion object {
        init {
            registerEffect(
                EffAssertWorldExists::class.java,
                "(assert|ensure|verify) (slimeworld|slime world) named %string% (0:exists|1:does(n't| not) exist) (in|from) %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>
    private var expectExists = true

    override fun toString(event: Event?, debug: Boolean): String {
        val state = if (expectExists) "exists" else "does not exist"
        return "assert slime world named ${worldName.toString(event, debug)} $state in ${loader.toString(event, debug)}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        worldName = expressions[0] as? Expression<String> ?: return false
        loader = expressions[1] as? Expression<SlimeLoader> ?: return false
        expectExists = parseResult.mark == 0
        return true
    }

    override fun execute(event: Event) {
        val name = worldName.getSingle(event) ?: return
        val slimeLoader = loader.getSingle(event) ?: return

        try {
            validateWorldName(name)
            val exists = slimeLoader.worldExists(name)
            if (exists != expectExists) {
                val message = if (expectExists) {
                    "World '$name' does not exist in the specified loader."
                } else {
                    "World '$name' already exists in the specified loader."
                }
                this.error(message)
            }
        } catch (exception: Exception) {
            this.error(userFacingError(exception))
        }
    }
}
