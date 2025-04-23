package dev.danielmillar.slimelink.skript.expressions

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
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
import dev.danielmillar.slimelink.util.Util
import org.bukkit.event.Event

@Name("Change Slime Properties")
@Description("Modify a property value in a SlimePropertyMap.")
@Examples(
    value = [
        "set pvp of {_slimeProperty} to true",
        "set spawn x of {_slimeProperty} to 100"
    ]
)
@Since("1.0.0")
class ExprModifySlimeProperties : SimpleExpression<Any>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprModifySlimeProperties::class.java,
                Any::class.java,
                ExpressionType.SIMPLE,
                "%slimeproperty% of %slimepropertymap%"
            )
        }
    }

    private lateinit var slimeProperties: Expression<SlimePropertyMap>
    private lateinit var slimePropertyType: Expression<SlimePropertiesEnum>

    override fun toString(event: Event?, debug: Boolean): String {
        return "${slimePropertyType.toString(event, debug)} of ${slimeProperties.toString(event, debug)}"
    }

    @Suppress("unchecked_cast")
    override fun init(
        expressions: Array<Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean {
        slimePropertyType = expressions[0] as Expression<SlimePropertiesEnum>
        slimeProperties = expressions[1] as Expression<SlimePropertyMap>
        return true
    }

    override fun acceptChange(mode: Changer.ChangeMode): Array<Class<*>> {
        return when(mode){
            Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> {
                arrayOf(Any::class.java)
            }
            else -> {
                Skript.error("Cannot $mode a property type")
                emptyArray()
            }
        }
    }

    @Suppress("unchecked_cast")
    override fun change(event: Event, delta: Array<out Any?>?, mode: Changer.ChangeMode) {
        val properties = slimeProperties.getSingle(event) ?: run {
            Skript.error("Provided slime properties is null")
            return
        }

        val property = slimePropertyType.getSingle(event) ?: run {
            Skript.error("Slime property is null")
            return
        }

        if (delta == null || delta.isEmpty()) {
            Skript.error("No value provided")
            return
        }

        val value = delta[0] ?: run {
            Skript.error("Provided value is null")
            return
        }

        when (property.dataType) {
            "String" -> {
                val prop = property.prop as SlimeProperty<String, *>
                if (mode == Changer.ChangeMode.SET) {
                    properties.setValue(prop, value.toString())
                } else {
                    Skript.error("Can only SET string properties, not ADD or REMOVE")
                }
            }
            "Integer" -> {
                val prop = property.prop as SlimeProperty<Int, *>
                val convertedValue = Util.anyToInt(value) ?: run {
                    Skript.error("Expected an Int value for property ${property.name} but got ${value::class.simpleName} instead")
                    return
                }

                when (mode) {
                    Changer.ChangeMode.SET -> properties.setValue(prop, convertedValue)
                    Changer.ChangeMode.ADD -> properties.setValue(prop, properties.getValue(prop) + convertedValue)
                    Changer.ChangeMode.REMOVE -> properties.setValue(prop, properties.getValue(prop) - convertedValue)
                    else -> {}
                }
            }
            "Float" -> {
                val prop = property.prop as SlimeProperty<Float, *>
                val convertedValue = Util.anyToFloat(value) ?: run {
                    Skript.error("Expected a Float value for property ${property.name} but got ${value::class.simpleName} instead")
                    return
                }

                when (mode) {
                    Changer.ChangeMode.SET -> properties.setValue(prop, convertedValue)
                    Changer.ChangeMode.ADD -> properties.setValue(prop, properties.getValue(prop) + convertedValue)
                    Changer.ChangeMode.REMOVE -> properties.setValue(prop, properties.getValue(prop) - convertedValue)
                    else -> {}
                }
            }
            "Boolean" -> {
                val prop = property.prop as SlimeProperty<Boolean, *>
                if (mode == Changer.ChangeMode.SET) {
                    val convertedValue = Util.anyToBoolean(value) ?: run {
                        Skript.error("Expected a Boolean value for property ${property.name} but got ${value::class.simpleName} instead")
                        return
                    }
                    properties.setValue(prop, convertedValue)
                } else {
                    Skript.error("Can only SET boolean properties, not ADD or REMOVE")
                }
            }
            else -> Skript.error("Unknown property data type: ${property.dataType}")
        }
    }

    override fun isSingle(): Boolean {
        return true
    }

    override fun getReturnType(): Class<Any> {
        return Any::class.java
    }

    @Suppress("unchecked_cast")
    override fun get(event: Event): Array<Any> {
        val properties = slimeProperties.getSingle(event) ?: return emptyArray()
        val property = slimePropertyType.getSingle(event) ?: return emptyArray()

        return when (property.dataType) {
            "String" -> {
                val prop = property.prop as SlimeProperty<String, *>
                arrayOf(properties.getValue(prop))
            }
            "Integer" -> {
                val prop = property.prop as SlimeProperty<Int, *>
                arrayOf(properties.getValue(prop))
            }
            "Float" -> {
                val prop = property.prop as SlimeProperty<Float, *>
                arrayOf(properties.getValue(prop))
            }
            "Boolean" -> {
                val prop = property.prop as SlimeProperty<Boolean, *>
                arrayOf(properties.getValue(prop))
            }
            else -> {
                Skript.error("Unknown property data type: ${property.dataType}")
                emptyArray()
            }
        }
    }
}
