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
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotLoaded
import org.bukkit.Bukkit
import org.bukkit.event.Event
import kotlin.system.measureTimeMillis

@Name("Clone Slime World")
@Description(
    value = [
        "This effect allows you to clone an existing SlimeWorld into another.",
        "You may also specify a data source if you're cloning to a different one from the original world."
    ]
)
@Examples(
    value = [
        "clone slimeworld from \"Test\" to \"TestCopy\"",
        "clone slime world from \"MyWorld\" to \"MyWorldBackup\" with datasource mysql",
    ]
)
@Since("1.0.0")
class EffCloneWorld : Effect() {

	companion object{
		init {
			Skript.registerEffect(
				EffCloneWorld::class.java,
				"clone (slimeworld|slime world) from %string% to %string% [newsource: with [datasource|data source] %-slimeloader%]"
			)
		}
	}

	private lateinit var sourceWorldName: Expression<String>
	private lateinit var targetWorldName: Expression<String>
	private var isNewSource = false
	private var loaderType: Expression<SlimeLoaderTypeEnum>? = null

	override fun toString(event: Event?, debug: Boolean): String? {
		val source = sourceWorldName.toString(event, debug)
		val target = targetWorldName.toString(event, debug)
		val loader = loaderType?.toString(event, debug) ?: ""
		return "Clone slime world from $source to $target ${if (isNewSource) "with data source $loader" else ""}"
	}

	@Suppress("unchecked_cast")
	override fun init(
		expressions: Array<out Expression<*>?>,
		matchedPattern: Int,
		isDelayed: Kleenean?,
		parseResult: SkriptParser.ParseResult?
	): Boolean {
		sourceWorldName = expressions[0] as Expression<String>
		targetWorldName = expressions[1] as Expression<String>
		isNewSource = matchedPattern == 0
		if (expressions.size > 2 && expressions[2] != null) {
			loaderType = expressions[2] as Expression<SlimeLoaderTypeEnum>
		}
		return true
	}

	override fun execute(event: Event?) {
		val sourceName = sourceWorldName.getSingle(event) ?: return
		val targetName = targetWorldName.getSingle(event) ?: return
		val loader = if (isNewSource) {
			loaderType?.getSingle(event)
		} else {
			null
		}

		try {
			requireWorldNotLoaded(targetName)

			val sourceWorldData = requireWorldDataExists(sourceName)

			if (sourceName.equals(targetName, ignoreCase = true)) {
				throw IllegalArgumentException("Source and target world names must be different")
			}

			val sourceWorldLoader = requireLoader((SlimeLoaderTypeEnum.fromId(sourceWorldData.getSource())!!))

			val datasource = if (loader == null) {
				sourceWorldLoader
			} else {
				requireLoader(loader)
			}

			Bukkit.getScheduler().runTaskAsynchronously(SlimeLink.getInstance(), Runnable {
				val time = measureTimeMillis {
					val slimeWorld = SlimeLink.getASP().readWorld(
						datasource,
						sourceName,
						false,
						sourceWorldData.toPropertyMap()
					)

					val clonedSlimeWorld = slimeWorld.clone(targetName, datasource)

					Bukkit.getScheduler().runTask(SlimeLink.getInstance(), Runnable {
						SlimeLink.getASP().loadWorld(clonedSlimeWorld, true)

						ConfigManager.getWorldConfig().setWorld(targetName, sourceWorldData)
						ConfigManager.saveWorldConfig()
					})
				}

				Skript.info("Cloned world '$sourceName' to '$targetName' in $time ms")
			})
		} catch (slime: SlimeException) {
			when (slime) {
				is WorldAlreadyExistsException -> {
					Skript.error("Failed to clone world '$sourceName' to '$targetName'. World already exists")
					slime.printStackTrace()
				}

				is CorruptedWorldException -> {
					Skript.error("Failed to clone world '$sourceName' to '$targetName'. World is corrupted")
					slime.printStackTrace()
				}

				is NewerFormatException -> {
					Skript.error("Failed to clone world '$sourceName' to '$targetName'. World is in a newer format")
					slime.printStackTrace()
				}

				is UnknownWorldException -> {
					Skript.error("Failed to clone world '$sourceName' to '$targetName'. World is unknown")
					slime.printStackTrace()
				}

				else -> {
					Skript.error("Failed to clone world '$sourceName' to '$targetName': ${slime.message}")
					slime.printStackTrace()
				}
			}
		} catch (e: IllegalArgumentException) {
			Skript.error(e.message)
		}
	}
}