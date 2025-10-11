package dev.danielmillar.slimelink.skript.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.slime.SlimeManager
import org.bukkit.event.Event

@Name("Initialize Loader")
@Description(
    value = [
        "**IMPORTANT**",
        "This effect initializes the Slime loader with the specified loader type.",
        "This must be ran at least once for anything to work."
    ]
)
@Since("1.0.0")
@Examples(
    value = [
        "initialize slime loader with datasource file",
        "initialize slime loader with datasource mysql",
        "initialize slime loader with datasource mongodb",
        "initialize slime loader with file",
        "initialize slime loader with mysql",
        "initialize slime loader with mongodb"
    ]
)
class EffInitializeLoader : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffInitializeLoader::class.java,
                "initialize slime loader with [datasource|data source] %slimeloader%"
            )
        }
    }

    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun execute(event: Event) {
        val loaderTypeValue = loaderType.getSingle(event) ?: return
        try {
            val success = SlimeManager.registerLoader(loaderTypeValue)
            if (!success || SlimeManager.getLoader(loaderTypeValue) == null) {
                Skript.error(
                    "An error occurred while trying to initialize Slime Loader. Loader datasource: ${loaderTypeValue.name.lowercase()}. Make sure this datasource is enabled and the credentials are correct, if applicable."
                )
            }
        } catch (ex: Exception) {
            Skript.error("Failed to initialize Slime Loader: ${ex.message}.")
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "initialize slime loader with datasource ${loaderType.toString(event, debug).lowercase()}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult?
    ): Boolean {
        loaderType = expressions[0] as Expression<SlimeLoaderTypeEnum>
        return true
    }
}
