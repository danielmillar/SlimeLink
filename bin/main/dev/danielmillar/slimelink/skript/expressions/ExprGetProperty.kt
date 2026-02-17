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

@Name("SlimeWorld - Get Property")
@Description(
    "Retrieves the value of a specific SlimeWorld property from a SlimePropertyMap.",
)
@Examples(
    value = [
        "set {_spawnX} to spawn_x of {_properties}",
        "set {_pvp} to pvp of {_slimeProps}",
        "broadcast \"%difficulty of {_properties}%\""
    ]
)
@Since("2.0.0")
class ExprGetProperty : SimpleExpression<Any>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprGetProperty::class.java,
                Any::class.java,
                ExpressionType.PROPERTY,
                "%slimeproperty% of %slimepropertymap%"
            )
        }
    }

    private lateinit var property: Expression<SlimePropertiesEnum>
    private lateinit var propertyMap: Expression<SlimePropertyMap>

    override fun toString(event: Event?, debug: Boolean): String {
        return "${property.toString(event, debug)} of ${propertyMap.toString(event, debug)}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean {
        property = expressions[0] as Expression<SlimePropertiesEnum>
        propertyMap = expressions[1] as Expression<SlimePropertyMap>
        return true
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<Any> = Any::class.java

    @Suppress("UNCHECKED_CAST")
    override fun get(event: Event): Array<Any> {
        val prop = property.getSingle(event) ?: return emptyArray()
        val map = propertyMap.getSingle(event) ?: return emptyArray()

        val slimeProp = prop.prop as SlimeProperty<Any, *>
        val value = map.getValue(slimeProp) ?: return emptyArray()
        
        return arrayOf(value)
    }
}
