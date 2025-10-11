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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import org.bukkit.Bukkit
import org.bukkit.event.Event

@Name("Fetch Loaded Slime Worlds")
@Description("This expressions returns all SlimeWorlds that are currently loaded on the server.")
@Examples(
    value = [
        "set {loadedSlimeWorlds::*} to all loaded slimeworlds with datasource %file%",
        "set {loadedSlimeWorlds::*} to all loaded slime worlds with %file%"
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

        try {
            val loader = requireLoader(loaderTypeValue)

            val loaderWorlds = try {
                loader.listWorlds()
            } catch (ex: Exception) {
                throw IllegalArgumentException("Failed to list worlds from loader ${loaderTypeValue.name}: ${ex.message}")
            }

            val loadedWorlds = Bukkit.getWorlds().map { it.name }

            return loaderWorlds.filter { loadedWorlds.contains(it) }.toTypedArray()
        }catch (e: IllegalArgumentException){
            Skript.error(e.message)
            return emptyArray()
        }
    }
}
