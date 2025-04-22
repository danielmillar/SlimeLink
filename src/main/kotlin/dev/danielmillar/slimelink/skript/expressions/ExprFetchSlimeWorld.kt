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
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.Event

@Name("Fetch Slime World")
@Description("Fetch a slime world using the specified world name.")
@Examples(
    value = [
        "set {_slimeWorld} to fetch slime world named \"Test\""
    ]
)
@Since("1.0.0")
class ExprFetchSlimeWorld : SimpleExpression<World>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprFetchSlimeWorld::class.java,
                World::class.java,
                ExpressionType.COMBINED,
                "fetch (slimeworld|slime world) named %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Fetching ${worldName.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parser: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        return true
    }

    override fun isSingle(): Boolean {
        return true
    }

    override fun getReturnType(): Class<World> {
        return World::class.java
    }

    override fun get(event: Event): Array<World> {
        val worldNameValue = worldName.getSingle(event) ?: return emptyArray()

        val bukkitWorld = Bukkit.getWorld(worldNameValue)
        if (bukkitWorld == null) {
            Skript.error("World $worldNameValue cannot be found, perhaps it doesn't exist or you didn't load it")
            return emptyArray()
        }

        return arrayOf(bukkitWorld)
    }
}
