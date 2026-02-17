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
import com.infernalsuite.asp.api.world.properties.SlimeProperty
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.slime.SlimePropertiesEnum
import org.bukkit.event.Event

@Name("SlimeWorld - Set Property")
@Description(
    "Sets the value of a specific SlimeWorld property on a SlimePropertyMap.",
    "The provided value must match the expected type for the property, mismatched types will produce an error.",
)
@Examples(
    value = [
        "set spawn_x of {_properties} to 100",
        "set pvp of {_slimeProps} to false",
        "set difficulty of {_properties} to \"hard\""
    ]
)
@Since("2.0.0")
class EffSetProperty : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffSetProperty::class.java,
                "set %slimeproperty% of %slimepropertymap% to %object%"
            )
        }
    }

    private lateinit var property: Expression<SlimePropertiesEnum>
    private lateinit var propertyMap: Expression<SlimePropertyMap>
    private lateinit var value: Expression<Any>

    override fun toString(event: Event?, debug: Boolean): String {
        return "set ${property.toString(event, debug)} of ${propertyMap.toString(event, debug)} to ${value.toString(event, debug)}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        property = expressions[0] as Expression<SlimePropertiesEnum>
        propertyMap = expressions[1] as Expression<SlimePropertyMap>
        value = expressions[2] as Expression<Any>
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun execute(event: Event) {
        val prop = property.getSingle(event) ?: return
        val map = propertyMap.getSingle(event) ?: return
        val newValue = value.getSingle(event) ?: return

        try {
            val slimeProp = prop.prop as SlimeProperty<Any, *>
            map.setValue(slimeProp, newValue)
        } catch (e: ClassCastException) {
            Skript.error("Invalid value type for property ${prop.name}: expected ${prop.dataType}")
        }
    }
}
