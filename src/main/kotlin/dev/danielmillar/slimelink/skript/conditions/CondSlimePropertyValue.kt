package dev.danielmillar.slimelink.skript.conditions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.infernalsuite.asp.api.world.properties.SlimeProperty
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.slime.SlimePropertiesEnum
import dev.danielmillar.slimelink.util.Util
import org.bukkit.event.Event
import kotlin.jvm.java

@Name("Slime Property Value")
@Description("Check the value of a SlimeProperty in a SlimePropertyMap.")
@Examples(
    value = [
        "if slime property spawn x of slimePropertyMap is 5",
        "if slime property allow monsters of slimePropertyMap isn't true"
    ]
)
@Since("1.0.0")
class CondSlimePropertyValue : Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondSlimePropertyValue::class.java,
                "slime property %slimeproperty% of %slimepropertymap% (is|1:is(n't| not)) %object%"
            )
        }
    }

    private lateinit var slimePropertyType: Expression<SlimePropertiesEnum>
    private lateinit var slimeProperties: Expression<SlimePropertyMap>
    private lateinit var value: Expression<Object>

    override fun toString(event: Event?, debug: Boolean): String {
        return "${slimePropertyType.toString(event, debug)} of ${slimeProperties.toString(event, debug)} ${if (isNegated) "isn't" else "is"} ${
            value.toString(
                event,
                debug
            )
        }"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        slimePropertyType = expressions[0] as Expression<SlimePropertiesEnum>
        slimeProperties = expressions[1] as Expression<SlimePropertyMap>
        value = LiteralUtils.defendExpression(expressions[2])
        isNegated = parseResult.hasTag("1")
        return LiteralUtils.canInitSafely(value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun check(event: Event?): Boolean {
        if (event == null) return false

        val valueObj = value.getSingle(event) ?: return false
        val properties = slimeProperties.getSingle(event)
        if (properties == null) {
            Skript.error("Provided slime properties is null")
            return false
        }

        val property = slimePropertyType.getSingle(event)
        if (property == null) {
            Skript.error("Slime property is null")
            return false
        }

        val converters = mapOf<String, (Any) -> Any?>(
            "String" to { it.toString() },
            "Integer" to { Util.anyToInt(it) },
            "Float" to { Util.anyToFloat(it) },
            "Boolean" to { Util.anyToBoolean(it) }
        )

        val converter = converters[property.dataType] ?: return false

        val compareValue = converter(valueObj)

        if (compareValue == null && property.dataType != "String") {
            Skript.error("Expected a ${property.dataType} value for property ${property.name} but got ${valueObj::class.simpleName} instead")
            return false
        }

        fun <T> compareProperty(prop: SlimeProperty<T, *>, value: Any?): Boolean {
            val propValue = properties.getValue(prop)
            return if (isNegated) propValue != value else propValue == value
        }

        return when (property.dataType) {
            "String" -> compareProperty(property.prop as SlimeProperty<String, *>, compareValue)
            "Integer" -> compareProperty(property.prop as SlimeProperty<Int, *>, compareValue)
            "Float" -> compareProperty(property.prop as SlimeProperty<Float, *>, compareValue)
            "Boolean" -> compareProperty(property.prop as SlimeProperty<Boolean, *>, compareValue)
            else -> false
        }
    }
}
