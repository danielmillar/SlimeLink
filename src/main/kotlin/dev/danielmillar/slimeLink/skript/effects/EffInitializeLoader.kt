package dev.danielmillar.slimeLink.skript.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import dev.danielmillar.slimeLink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimeLink.slime.SlimeManager
import org.bukkit.event.Event

@Name("Initialize Loader")
@Description("Initializes the loader for the given loader type.")
@Since("1.0.0")
class EffInitializeLoader : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffInitializeLoader::class.java,
                "initialize slime loader with [type] %slimeloader%"
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
                    "An error occurred while trying to initialize Slime Loader. Loader type: ${loaderTypeValue.name.lowercase()}. Make sure this loader is enabled and the credentials are correct, if applicable."
                )
            }
        } catch (ex: Exception) {
            Skript.error("Failed to initialize Slime Loader: ${ex.message}")
            ex.printStackTrace()
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "initialize slime loader with type ${loaderType.toString(event, debug)}"
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
