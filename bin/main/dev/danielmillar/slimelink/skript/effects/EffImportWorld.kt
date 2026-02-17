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
import com.infernalsuite.asp.api.loaders.SlimeLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.importSlimeWorldFromVanillaWorld
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.Bukkit
import org.bukkit.event.Event
import java.io.File

@Name("SlimeWorld - Import Vanilla World")
@Description(
    "Imports a vanilla Minecraft world into a Slime world using the provided loader.",
    "This saves and loads the imported world."
)
@Examples(
    value = [
        "import vanilla world \"world\" as \"slime_world\" with {_loader}",
    ]
)
@Since("2.0.0")
class EffImportWorld : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffImportWorld::class.java,
                "import vanilla world %string% as %string% with %slimeloader%"
            )
        }
    }

    private lateinit var vanillaWorldName: Expression<String>
    private lateinit var slimeWorldName: Expression<String>
    private lateinit var loader: Expression<SlimeLoader>

    override fun toString(event: Event?, debug: Boolean): String =
        "import vanilla world ${vanillaWorldName.toString(event, debug)} as ${slimeWorldName.toString(event, debug)}"

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        vanillaWorldName = expressions[0] as Expression<String>
        slimeWorldName = expressions[1] as Expression<String>
        loader = expressions[2] as Expression<SlimeLoader>
        return true
    }

    override fun execute(event: Event) {
        val vanillaName = vanillaWorldName.getSingle(event) ?: return
        val slimeName = slimeWorldName.getSingle(event) ?: return
        val slimeLoader = loader.getSingle(event) ?: return

        try {
            validateWorldName(slimeName)
            requireWorldNotLoaded(slimeName)
            
            val worldFolder = File(Bukkit.getWorldContainer(), vanillaName)
            require(worldFolder.exists() && worldFolder.isDirectory) {
                "Vanilla world '$vanillaName' not found at ${worldFolder.absolutePath}"
            }
            
            importSlimeWorldFromVanillaWorld(worldFolder, slimeName, slimeLoader)
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        }
    }
}
