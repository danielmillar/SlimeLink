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
import com.infernalsuite.asp.api.exceptions.*
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.util.SlimeWorldUtils.importSlimeWorldFromVanillaWorld
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataNotExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.validateWorldName
import org.bukkit.event.Event
import java.io.File

@Name("Import SlimeWorld")
@Description(
	value = [
		"Import a vanilla world as a SlimeWorld.",
		"Default Slime Properties values will be used for the imported world."
	]
)
@Examples(
	value = [
		"import world with path \"/home/container/world\" as slimeworld named \"worldimport\" with datasource file"
	]
)
@Since("1.1.2")
class EffImportWorld : Effect() {

	companion object {
		init {
			Skript.registerEffect(
				EffImportWorld::class.java,
				"import world with path %string% as (slimeworld|slime world) named %string% with [datasource|data source] %slimeloader%"
			)
		}
	}

	private lateinit var vanillaWorldPath: Expression<String>
	private lateinit var slimeWorldName: Expression<String>
	private lateinit var loaderType: Expression<SlimeLoaderTypeEnum>

	override fun toString(event: Event?, debug: Boolean): String {
		val path = vanillaWorldPath.toString(event, debug)
		val name = slimeWorldName.toString(event, debug)
		val loader = loaderType.toString(event, debug)
		return "Import world with path $path as slimeworld named $name with datasource $loader"
	}

	@Suppress("unchecked_cast")
	override fun init(
		expressions: Array<Expression<*>?>,
		matchedPattern: Int,
		isDelayed: Kleenean,
		parseResult: SkriptParser.ParseResult
	): Boolean {
		vanillaWorldPath = expressions[0] as Expression<String>
		slimeWorldName = expressions[1] as Expression<String>
		loaderType = expressions[2] as Expression<SlimeLoaderTypeEnum>
		return true
	}

	override fun execute(event: Event?) {
		val sourcePath = vanillaWorldPath.getSingle(event) ?: return
		val targetName = slimeWorldName.getSingle(event) ?: return
		val loaderType = loaderType.getSingle(event) ?: return

		try {
			val loader = requireLoader(loaderType)

			validateWorldName(targetName)
			requireWorldDataNotExists(targetName)
			requireWorldNotExists(targetName)

			val worldDir = File(sourcePath)
			if (!worldDir.exists() || !worldDir.isDirectory) {
				throw IllegalArgumentException("The specified world path does not exist or is not a directory: $sourcePath")
			}

			importSlimeWorldFromVanillaWorld(worldDir, targetName, loaderType, loader)
		} catch (slime: SlimeException) {
			when (slime) {
				is WorldAlreadyExistsException -> {
					Skript.error("Failed to import vanilla world at '$sourcePath' to '$targetName'. World already exists")
					slime.printStackTrace()
				}

				is InvalidWorldException -> {
					Skript.error("Failed to import vanilla world at '$sourcePath' to '$targetName'. World is invalid")
					slime.printStackTrace()
				}

				is WorldLoadedException -> {
					Skript.error("Failed to import vanilla world at '$sourcePath' to '$targetName'. World is loaded")
					slime.printStackTrace()
				}

				is WorldTooBigException -> {
					Skript.error("Failed to import vanilla world at '$sourcePath' to '$targetName'. World is too big")
					slime.printStackTrace()
				}

				else -> {
					Skript.error("An unknown error occurred while importing vanilla world at '$sourcePath' to '$targetName': ${slime.message}")
					slime.printStackTrace()
				}
			}
		} catch (e: IllegalArgumentException) {
			Skript.error(e.message)
		}
	}
}