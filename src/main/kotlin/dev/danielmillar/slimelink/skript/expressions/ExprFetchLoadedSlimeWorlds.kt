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
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.event.Event

@Name("Fetch Loaded Slime Worlds")
@Description("List all loaded SlimeWorlds for a specific loader type. ")
@Examples(
    value = [
        "set {loadedSlimeWorlds::*} to all loaded slimeworlds with datasource %file%",
        "set {loadedSlimeWorlds::*} to all loaded slime worlds with %file%",
        "set {worlds} to all loaded slimeworlds with type %mysql%"
    ]
)
@Since("1.0.0")
class ExprFetchLoadedSlimeWorlds : SimpleExpression<String>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprFetchLoadedSlimeWorlds::class.java,
                String::class.java,
                ExpressionType.SIMPLE,
                "all loaded (slimeworlds|slime worlds) with [datasource|data source] %slimeloader%"
            )
        }
    }

    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Fetching all loaded slime worlds with datasource ${loaderType.toString(event, debug)}"
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

    override fun isSingle(): Boolean {
        return false
    }

    override fun getReturnType(): Class<String> {
        return String::class.java
    }

    override fun get(event: Event): Array<String> {
        val loaderTypeValue = loaderType.getSingle(event) ?: return emptyArray()

        val loader = SlimeManager.getLoader(loaderTypeValue)
        if (loader == null) {
            Skript.error("Loader ${loaderTypeValue.name} is not registered. Please initialize it first.")
            return emptyArray()
        }

        val loaderWorlds = try {
            loader.listWorlds()
        } catch (ex: Exception) {
            Skript.error("Failed to list worlds from loader ${loaderTypeValue.name}: ${ex.message}")
            return emptyArray()
        }

        val loadedWorlds = Bukkit.getWorlds().map { it.name }

        return loaderWorlds.filter { loadedWorlds.contains(it) }.toTypedArray()
    }
}
