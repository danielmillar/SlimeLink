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
import dev.danielmillar.slimelink.slime.SlimeLoaderType
import org.bukkit.event.Event

@Name("SlimeWorld - Create Loader")
@Description(
    "Creates a new SlimeLoader instance for the specified loader type (file, MySQL, or MongoDB).",
)
@Examples(
    value = [
        "set {_loader} to new slime loader from file",
        "set {_loader} to slime loader from mysql",
        "set {_loader} to new slime loader from mongodb"
    ]
)
@Since("2.0.0")
class ExprCreateLoader : SimpleExpression<SlimeLoader>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprCreateLoader::class.java,
                SlimeLoader::class.java,
                ExpressionType.SIMPLE,
                "[new] slime loader from (file|mysql|mongodb)"
            )
        }
    }

    private lateinit var loaderType: SlimeLoaderType

    override fun toString(event: Event?, debug: Boolean): String =
        "new slime loader from ${loaderType.id}"

    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean {
        loaderType = when (parser.mark) {
            0 -> SlimeLoaderType.FILE
            1 -> SlimeLoaderType.MYSQL
            2 -> SlimeLoaderType.MONGODB
            else -> return false
        }
        return true
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<SlimeLoader> = SlimeLoader::class.java

    override fun get(event: Event): Array<SlimeLoader> {
        return try {
            arrayOf(loaderType.createLoader())
        } catch (e: IllegalStateException) {
            Skript.error(e.message)
            emptyArray()
        }
    }
}
