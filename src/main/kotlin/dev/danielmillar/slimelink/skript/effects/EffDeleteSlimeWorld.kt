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
import com.infernalsuite.asp.api.exceptions.UnknownWorldException
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.event.Event
import java.io.IOException
import kotlin.system.measureTimeMillis

@Name("Delete Slime World")
@Description("Delete a Slime World with a specified name.")
@Examples(
    value = [
        "delete slimeworld named \"Test\" with type %file%",
        "delete slime world named \"MyWorld\" with %file%",
        "delete slimeworld named \"OldWorld\" with type %mysql%"
    ]
)
@Since("1.0.0")
class EffDeleteSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffDeleteSlimeWorld::class.java,
                "delete (slimeworld|slime world) named %string% with [type] %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Delete slime world ${worldName.toString(event, debug)} with type ${loaderType.toString(event, debug)}"
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

        if (!ConfigManager.getWorldConfig().hasWorld(worldNameValue)) {
            Skript.error("Can't find world $worldNameValue in config!")
            return
        }

        val bukkitWorld = Bukkit.getWorld(worldNameValue)
        if (bukkitWorld != null) {
            Skript.error("World $worldNameValue is loaded, can't delete. Try unload world first")
            return
        }

        val loader = SlimeManager.getLoader(loaderTypeValue)
        if (loader == null) {
            Skript.error("Loader ${loaderTypeValue.name} is not registered. Please initialize it first.")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(SlimeLink.getInstance(), Runnable {
            try {
                val timeTaken = measureTimeMillis {
                    loader.deleteWorld(worldNameValue)

                    ConfigManager.getWorldConfig().removeWorld(worldNameValue)
                    ConfigManager.saveWorldConfig()
                }

                Skript.info("Successfully deleted world $worldNameValue within $timeTaken ms!")
            } catch (ex: Exception) {
                when (ex) {
                    is IOException -> {
                        Skript.error("Failed to delete world $worldNameValue. Check logger for more information")
                        ex.printStackTrace()
                    }
                    is UnknownWorldException -> {
                        Skript.error("Datasource doesn't contain any world called $worldNameValue")
                        ex.printStackTrace()
                    }
                    else -> {
                        Skript.error("Failed to delete world $worldNameValue: ${ex.message}")
                        ex.printStackTrace()
                    }
                }
            }
        })
    }
}
