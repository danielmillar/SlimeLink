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
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException
import com.infernalsuite.asp.api.exceptions.NewerFormatException
import com.infernalsuite.asp.api.exceptions.SlimeException
import com.infernalsuite.asp.api.exceptions.UnknownWorldException
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.util.SlimeWorldUtils.loadWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event
import java.io.IOException

@Name("Load Slime World")
@Description(
    value = [
        "This effect allows you to load a specific SlimeWorld.",
        "The world must be created before it can be loaded."
    ]
)
@Examples(
    value = [
        "load slimeworld named \"MyWorld\"",
        "load slime world named \"MyWorld\"",
    ]
)
@Since("1.0.0")
class EffLoadSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffLoadSlimeWorld::class.java,
                "load (slimeworld|slime world) named %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Load slime world ${worldName.toString(event, debug)}"
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
            validateWorldName(worldNameValue)

            requireWorldNotLoaded(worldNameValue)
            val worldData = requireWorldDataExists(worldNameValue)
            val loader = requireLoader(SlimeLoaderTypeEnum.fromId(worldData.getSource())!!)

            loadWorldAsync(worldNameValue, loader, worldData.isReadOnly(), worldData.toPropertyMap())
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        } catch (io: IOException) {
            Skript.error("I/O error while loading world $worldNameValue: ${io.message}")
            io.printStackTrace()
        } catch (slime: SlimeException) {
            when (slime) {
                is CorruptedWorldException -> {
                    Skript.error("Failed to load world $worldNameValue. World seems to be corrupted")
                    slime.printStackTrace()
                }

                is NewerFormatException -> {
                    Skript.error("Failed to load world $worldNameValue. This world was serialized with a newer version of Slime Format that SWM can't understand")
                    slime.printStackTrace()
                }

                is UnknownWorldException -> {
                    Skript.error("Failed to load world $worldNameValue. World cannot be found")
                    slime.printStackTrace()
                }

                else -> {
                    Skript.error("Failed to load world $worldNameValue: ${slime.message}")
                    slime.printStackTrace()
                }
            }
        }
    }
}
