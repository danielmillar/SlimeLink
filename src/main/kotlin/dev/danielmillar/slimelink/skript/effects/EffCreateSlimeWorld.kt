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
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.util.SlimeWorldUtils.createAndLoadWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataNotExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotExists
import org.bukkit.event.Event
import java.io.IOException

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

        try {
            requireWorldNotExists(worldNameValue)
            requireWorldDataNotExists(worldNameValue)

            val loader = requireLoader(loaderTypeValue)

            createAndLoadWorldAsync(worldNameValue, properties, loader, loaderTypeValue.name.lowercase(), isReadOnly)
        }catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }catch (io: IOException) {
            Skript.error("I/O error while creating world '$worldNameValue': ${io.message}")
            io.printStackTrace()
        }
    }
}
