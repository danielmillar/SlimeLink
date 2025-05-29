package dev.danielmillar.slimelink.skript.sections

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimelink.util.SlimeWorldUtils.createAndLoadWorldAsync
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireLoader
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldDataNotExists
import dev.danielmillar.slimelink.util.SlimeWorldUtils.requireWorldNotExists
import org.bukkit.event.Event
import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData
import org.skriptlang.skript.lang.entry.util.LiteralEntryData
import java.io.IOException

@Name("Create Slime World")
@Description("Creates a new Slime World with the specified name, properties, and loader type. Optionally can be set to read-only.")
@Examples(
    value = [
        "create slime world named \"TestWorld\"",
        "\tloader: file",
        "\tproperties: {slimeProperties}",
        "\treadonly: true",
        "create new slime world named \"TestWorld\"",
        "\tloader: mongodb",
        "\tproperties: {slimeProperties}",
        "\treadonly: false"
    ]
)
@Since("1.0.0")
class SecCreateSlimeWorld : Section() {

    companion object {
        init {
            Skript.registerSection(
                SecCreateSlimeWorld::class.java,
                "create [a] [new] (slimeworld|slime world) (named|with name) %string%"
            )
        }

        private val ENTRY_VALIDATOR: EntryValidator = EntryValidator.builder()
            .addEntryData(LiteralEntryData("loader", null, false, SlimeLoaderTypeEnum::class.java))
            .addEntryData(ExpressionEntryData("properties", null, true, SlimePropertyMap::class.java))
            .addEntryData(LiteralEntryData("readonly", false, true, Boolean::class.javaObjectType))
            .build()
    }

    private lateinit var worldNameExpr: Expression<String>
    private lateinit var loaderType: SlimeLoaderTypeEnum
    private var propertiesExpr: Expression<SlimePropertyMap>? = null
    private var isReadOnly: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: List<TriggerItem>
    ): Boolean {
        ENTRY_VALIDATOR.validate(sectionNode)?.let { entry ->
            worldNameExpr = expressions[0] as Expression<String>
            val loaderVal = entry.getOptional("loader", false) as? SlimeLoaderTypeEnum
            if (loaderVal == null) {
                Skript.error("You must specify a valid loader type for the slime world.")
                return false
            }
            loaderType = loaderVal

            propertiesExpr = entry.getOptional("properties", true) as? Expression<SlimePropertyMap>
            isReadOnly = entry.get("readonly", Boolean::class.javaObjectType, true) ?: false

            return true
        }
        return false
    }

    override fun walk(event: Event): TriggerItem? {
        safeExecute(event)
        return super.walk(event, false)
    }

    private fun safeExecute(event: Event) {
        try {
            val worldName = worldNameExpr.getSingle(event)
                ?: throw IllegalArgumentException("World name cannot be null")
            val loader = requireLoader(loaderType)
            val props = propertiesExpr?.getSingle(event) ?: SlimePropertyMap()

            requireWorldNotExists(worldName)
            requireWorldDataNotExists(worldName)

            createAndLoadWorldAsync(
                worldName = worldName,
                properties = props,
                loader = loader,
                loaderName = loaderType.name.lowercase(),
                readOnly = isReadOnly
            )
        } catch (e: IllegalArgumentException) {
            Skript.error(e.message)
        } catch (io: IOException) {
            Skript.error("I/O error while creating world '${worldNameExpr}': ${io.message}")
            io.printStackTrace()
        }
    }

    override fun toString(event: Event?, debug: Boolean): String = buildString {
        append("Create slime world ${worldNameExpr.toString(event, debug)}")
        append(" with datasource ${loaderType.name.lowercase()}")
        propertiesExpr?.let { append(" and properties ${it.toString(event, debug)}") }
        if (isReadOnly) append(" as readOnly")
    }
}