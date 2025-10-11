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
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.saveWorldSync
import org.bukkit.event.Event

@Name("Save Slime World")
@Description("This effect allows you to save a specific SlimeWorld.")
@Examples(
    value = [
        "save slimeworld named \"MyWorld\"",
        "save slime world named \"MyWorld\"",
    ]
)
@Since("1.0.0")
class EffSaveSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffSaveSlimeWorld::class.java,
                "save (slimeworld|slime world) named %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Save slime world ${worldName.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        return true
    }

    override fun execute(event: Event) {
        val worldNameValue = worldName.getSingle(event) ?: return

        try {
            val world = requireWorldLoaded(worldNameValue)
            val worldData = requireWorldDataExists(worldNameValue)

            if (worldData.isReadOnly()) throw IllegalArgumentException("World $worldNameValue is read only")

            saveWorldSync(world, worldNameValue, worldData)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
