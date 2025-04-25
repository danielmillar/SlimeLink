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
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.config.WorldData
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.slime.SlimeManager
import org.bukkit.Bukkit
import org.bukkit.event.Event
import java.io.IOException
import kotlin.system.measureTimeMillis

@Name("Create Slime World")
@Description("Create a new Slime World with a name, slime properties, and whether it's readOnly.")
@Examples(
    value = [
        "create slimeworld named \"Test\" with props {globalProps} with datasource %file%",
        "new slime world named \"MyWorld\" with props {worldProps} with %file%",
        "create slimeworld named \"ReadOnlyWorld\" with props {minimalProps} as ReadOnly with datasource %file%",
        "new slime world named \"AnotherWorld\" with props {customProps} as ReadOnly with %mysql%"
    ]
)
@Since("1.0.0")
class EffCreateSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffCreateSlimeWorld::class.java,
                "(create|new) (slimeworld|slime world) named %string% with props %slimepropertymap% [readonly:as ReadOnly] with [datasource|data source] %slimeloader%"
            )
        }
    }

    private lateinit var worldName: Expression<String>
    private lateinit var slimeProperties: Expression<SlimePropertyMap>
    private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>
    private var isReadOnly = false

    override fun toString(event: Event?, debug: Boolean): String {
        return "Create slime world ${worldName.toString(event, debug)} with properties ${
            slimeProperties.toString(
                event,
                debug
            )
        } ${if (isReadOnly) "as readOnly" else ""} with datasource ${loaderType.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        worldName = expressions[0] as Expression<String>
        slimeProperties = expressions[1] as Expression<SlimePropertyMap>
        loaderType = expressions[2] as Expression<SlimeLoaderTypeEnum>
        isReadOnly = parseResult?.hasTag("readonly") ?: false
        return true
    }

    override fun execute(event: Event) {
        val worldNameValue = worldName.getSingle(event) ?: return
        val properties = slimeProperties.getSingle(event) ?: return
        val loaderTypeValue = loaderType.getSingle(event) ?: return

        val bukkitWorld = Bukkit.getWorld(worldNameValue)
        if (bukkitWorld != null) {
            Skript.error("A world with that name already exists!")
            return
        }

        val worldDataExists = ConfigManager.getWorldConfig().hasWorld(worldNameValue)
        if (worldDataExists) {
            Skript.error("World $worldNameValue already exists in config")
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
                    val slimeWorld = SlimeLink.getASP().createEmptyWorld(
                        worldNameValue,
                        isReadOnly,
                        properties,
                        loader
                    )
                    SlimeLink.getASP().saveWorld(slimeWorld)

                    Bukkit.getScheduler().runTask(SlimeLink.getInstance(), Runnable {
                        try {
                            SlimeLink.getASP().loadWorld(slimeWorld, true)

                            val worldData = WorldData(
                                source = loaderTypeValue.name.lowercase(),
                                readOnly = isReadOnly
                            )
                            ConfigManager.getWorldConfig().setWorld(worldNameValue, worldData)
                            ConfigManager.saveWorldConfig()
                        } catch (ex: Exception) {
                            when (ex) {
                                is IllegalArgumentException, is UnknownWorldException, is IOException -> {
                                    Skript.error("Failed to create/load world $worldNameValue: ${ex.message}")
                                }
                                else -> throw ex
                            }
                        }
                    })
                }

                Skript.info("Successfully created world $worldNameValue within $timeTaken ms!")
            } catch (ex: Exception) {
                when (ex) {
                    is WorldAlreadyExistsException -> {
                        Skript.error("Failed to create world $worldNameValue: world already exists")
                    }
                    is IOException -> {
                        Skript.error("Failed to create world $worldNameValue. Check logger for more information")
                        ex.printStackTrace()
                    }
                    else -> {
                        Skript.error("Failed to create world $worldNameValue: ${ex.message}")
                        ex.printStackTrace()
                    }
                }
            }
        })
    }
}
