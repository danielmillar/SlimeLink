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
import com.infernalsuite.asp.api.exceptions.SlimeException
import com.infernalsuite.asp.api.exceptions.UnknownWorldException
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.util.SlimeWorldUtils.deleteWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import org.bukkit.event.Event
import java.io.IOException

@Name("Delete Slime World")
@Description(
    value = [
        "This effect allows you to delete a specific SlimeWorld.",
        "The world must be unloaded before deletion can happen."
    ]
)
@Examples(
    value = [
        "delete slimeworld named \"Test\"",
        "delete slime world named \"MyWorld\""
    ]
)
@Since("1.0.0")
class EffDeleteSlimeWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffDeleteSlimeWorld::class.java,
                "delete (slimeworld|slime world) named %string%"
            )
        }
    }

    private lateinit var worldName: Expression<String>

    override fun toString(event: Event?, debug: Boolean): String {
        return "Delete slime world ${worldName.toString(event, debug)}"
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
            val worldData = requireWorldDataExists(worldNameValue)
            requireWorldNotLoaded(
                worldNameValue,
                "World $worldNameValue is currently loaded. Please unload it before deleting."
            )
            val loader = requireLoader(SlimeLoaderTypeEnum.fromId(worldData.getSource())!!)

            deleteWorldAsync(worldNameValue, loader)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        } catch (io: IOException) {
            Skript.error("I/O error while deleting world $worldNameValue: ${io.message}")
            io.printStackTrace()
        } catch (slime: SlimeException) {
            when (slime) {
                is UnknownWorldException -> {
                    Skript.error("Failed to delete world $worldNameValue. World does not exist")
                    slime.printStackTrace()
                }

                else -> {
                    Skript.error("Failed to delete world $worldNameValue: ${slime.message}")
                    slime.printStackTrace()
                }
            }
        }
    }
}
