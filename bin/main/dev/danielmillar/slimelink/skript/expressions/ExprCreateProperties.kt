package dev.danielmillar.slimelink.skript.expressions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.world.properties.SlimeProperty
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.slime.SlimePropertiesEnum
import org.bukkit.event.Event

@Name("SlimeWorld - Create Properties")
@Description(
    "Creates a new SlimePropertyMap populated with the default values for all SlimeWorld properties.",
)
@Examples(
    value = [
        "set {_props} to new slime world properties",
        "set {_props} to default slime world properties",
        "create slime world named \"MyWorld\" with {_loader} using (new slime world properties)"
    ]
)
@Since("2.0.0")
class ExprCreateProperties : SimpleExpression<SlimePropertyMap>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprCreateProperties::class.java,
                SlimePropertyMap::class.java,
                ExpressionType.SIMPLE,
                "[default|new] (slimeworld|slime world) properties"
            )
        }
    }

    override fun toString(event: Event?, debug: Boolean): String = "new slime world properties"

    override fun init(
        expressions: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean = true

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<SlimePropertyMap> = SlimePropertyMap::class.java

    @Suppress("UNCHECKED_CAST")
    override fun get(event: Event): Array<SlimePropertyMap> {
        val propertyMap = SlimePropertyMap()
        
        SlimePropertiesEnum.entries.forEach { entry ->
            val prop = entry.prop as SlimeProperty<Any?, *>
            propertyMap.setValue(prop, prop.defaultValue)
        }
        
        return arrayOf(propertyMap)
    }
}
