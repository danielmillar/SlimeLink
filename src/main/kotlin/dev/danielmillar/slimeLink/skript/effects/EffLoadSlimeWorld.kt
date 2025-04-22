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
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException
import com.infernalsuite.asp.api.exceptions.NewerFormatException
import com.infernalsuite.asp.api.exceptions.UnknownWorldException
import dev.danielmillar.slimeLink.SlimeLink
import dev.danielmillar.slimeLink.config.ConfigManager
import dev.danielmillar.slimeLink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimeLink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.event.Event
import java.io.IOException
import kotlin.system.measureTimeMillis

@Name("Load Slime World")
@Description("Load a new Slime World with a specified name.")
@Examples(
    value = [
        "load slime world named \"Test\" with type %file%"
    ]
)
@Since("1.0.0")
class EffLoadSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffLoadSlimeWorld::class.java,
                "load (slimeworld|slime world) named %string% with [type] %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Load slime world ${worldName.toString(event, debug)} with type ${loaderType.toString(event, debug)}"
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
        if (bukkitWorld != null) {
            Skript.error("World $worldNameValue is already loaded!")
            return
        }

        val worldData = ConfigManager.getWorldConfig().getWorld(worldNameValue)
        if (worldData == null) {
            Skript.error("World $worldNameValue cannot be found in config")
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
                    val slimeWorld = SlimeLink.getASP().readWorld(
                        loader,
                        worldNameValue,
                        worldData.isReadOnly(),
                        worldData.toPropertyMap()
                    )

                    Bukkit.getScheduler().runTask(SlimeLink.getInstance(), Runnable {
                        try {
                            SlimeLink.getASP().loadWorld(slimeWorld, true)
                        } catch (ex: Exception) {
                            when (ex) {
                                is IllegalArgumentException, is UnknownWorldException, is IOException -> {
                                    Skript.error("Failed to load world $worldNameValue: ${ex.message}")
                                }
                                else -> throw ex
                            }
                        }
                    })
                }

                Skript.info("World $worldNameValue loaded within $timeTaken ms!")
            } catch (ex: Exception) {
                when (ex) {
                    is CorruptedWorldException -> {
                        Skript.error("Failed to load world $worldNameValue. World seems to be corrupted")
                        ex.printStackTrace()
                    }
                    is NewerFormatException -> {
                        Skript.error("Failed to load world $worldNameValue. This world was serialized with a newer version of Slime Format that SWM can't understand")
                        ex.printStackTrace()
                    }
                    is UnknownWorldException -> {
                        Skript.error("Failed to load world $worldNameValue. World cannot be found")
                        ex.printStackTrace()
                    }
                    is IOException, is IllegalArgumentException -> {
                        Skript.error("Failed to load world $worldNameValue. Check logger for more information")
                        ex.printStackTrace()
                    }
                    else -> {
                        Skript.error("Failed to load world $worldNameValue: ${ex.message}")
                        ex.printStackTrace()
                    }
                }
            }
        })
    }
}
