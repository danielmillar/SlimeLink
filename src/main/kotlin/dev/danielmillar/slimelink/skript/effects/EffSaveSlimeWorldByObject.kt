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
import dev.danielmillar.slimelink.util.SlimeWorldUtils
import org.bukkit.World
import org.bukkit.event.Event

@Name("Save Slime World By Object")
@Description("Save a Slime World using a Bukkit World object.")
@Examples(
    value = [
        "save slimeworld {world} with datasource %file%",
        "save slime world {myWorld} with %file%",
        "save slimeworld {serverWorld} with datasource %mysql%"
    ]
)
@Since("1.0.0")
class EffSaveSlimeWorldByObject : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffSaveSlimeWorldByObject::class.java,
                "save (slimeworld|slime world) %world% with [datasource|data source] %slimeloader%"
            )
        }
    }

    private lateinit var world: Expression<World>
    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Save slime world ${world.toString(event, debug)} with type ${loaderType.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        world = expressions[0] as Expression<World>
        loaderType = expressions[1] as Expression<SlimeLoaderTypeEnum>
        return true
    }

    override fun execute(event: Event) {
        val bukkitWorld = world.getSingle(event) ?: return
        val worldNameValue = bukkitWorld.name
        val loaderTypeValue = loaderType.getSingle(event) ?: return

        // Get world data
        val worldData = SlimeWorldUtils.getWorldData(worldNameValue) ?: return

        // Check if world is read-only
        if (SlimeWorldUtils.isWorldReadOnly(worldNameValue, worldData)) {
            return
        }

        // Validate loader
        if (!SlimeWorldUtils.validateLoader(loaderTypeValue)) {
            return
        }

        // Save the world
        SlimeWorldUtils.saveWorld(worldNameValue, worldData)
    }
}
