package dev.danielmillar.slimeLink.skript.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.world.SlimeWorld
import dev.danielmillar.slimeLink.SlimeLink
import dev.danielmillar.slimeLink.config.ConfigManager
import dev.danielmillar.slimeLink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimeLink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.event.Event
import java.io.IOException
import kotlin.system.measureTimeMillis

@Name("Save Slime World")
@Description("Save a Slime World with a specified name.")
@Examples(
    value = [
        "save slime world named \"Test\" with type %file%"
    ]
)
@Since("1.0.0")
class EffSaveSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffSaveSlimeWorld::class.java,
                "save (slimeworld|slime world) named %string% with [type] %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Save slime world ${worldName.toString(event, debug)} with type ${loaderType.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        loaderType = expressions[1] as Expression<SlimeLoaderTypeEnum>
        return true
    }

    override fun execute(event: Event) {
        val worldNameValue = worldName.getSingle(event) ?: return
        val loaderTypeValue = loaderType.getSingle(event) ?: return

        val bukkitWorld = Bukkit.getWorld(worldNameValue)
        if (bukkitWorld == null) {
            Skript.error("World $worldNameValue is not loaded!")
            return
        }

        val worldData = ConfigManager.getWorldConfig().getWorld(worldNameValue)
        if (worldData == null) {
            Skript.error("World $worldNameValue cannot be found in config")
            return
        }

        if (worldData.isReadOnly()) {
            Skript.warning("World $worldNameValue readOnly property is true, can't save")
            return
        }

        val loader = SlimeManager.getLoader(loaderTypeValue)
        if (loader == null) {
            Skript.error("Loader ${loaderTypeValue.name} is not registered. Please initialize it first.")
            return
        }

        try {
            val timeTaken = measureTimeMillis {
                val loadedWorld = SlimeLink.getASP().getLoadedWorld(worldNameValue) as SlimeWorld
                SlimeLink.getASP().saveWorld(loadedWorld)

                ConfigManager.getWorldConfig().setWorld(worldNameValue, worldData)
                ConfigManager.saveWorldConfig()
            }

            Skript.info("World $worldNameValue saved within $timeTaken ms!")
        } catch (ex: Exception) {
            when (ex) {
                is IOException -> {
                    Skript.error("Failed to save world $worldNameValue. Check logger for more information")
                    ex.printStackTrace()
                }
                else -> {
                    Skript.error("Failed to save world $worldNameValue: ${ex.message}")
                    ex.printStackTrace()
                }
            }
        }
    }
}
