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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.saveWorldSync
import org.bukkit.World
import org.bukkit.event.Event

@Name("Save Slime World By Object")
@Description("Save a Slime World using a Bukkit World object.")
@Examples(
    value = [
        "save slimeworld {world}",
        "save slime world {myWorld}",
        "save slimeworld {serverWorld}"
    ]
)
@Since("1.0.0")
class EffSaveSlimeWorldByObject : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffSaveSlimeWorldByObject::class.java,
                "save (slimeworld|slime world) %world%"
            )
        }
    }

    private lateinit var world: Expression<World>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Save slime world ${world.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        world = expressions[0] as Expression<World>
        return true
    }

    override fun execute(event: Event) {
        val bukkitWorld = world.getSingle(event) ?: return
        val worldNameValue = bukkitWorld.name

        try {
            val worldData = requireWorldDataExists(worldNameValue)

            if (worldData.isReadOnly()) throw IllegalArgumentException("World $worldNameValue is read only")

            saveWorldSync(bukkitWorld, worldNameValue, worldData)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
