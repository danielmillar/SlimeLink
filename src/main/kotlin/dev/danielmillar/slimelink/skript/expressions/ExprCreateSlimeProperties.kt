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
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimelink.slime.SlimePropertiesEnum
import org.bukkit.event.Event

@Name("Create Slime Properties")
@Description("Create a SlimePropertyMap with default values.")
@Examples(
    value = [
        "set {_slimeProperty} to slime world properties",
        "set {_props} to default slime world properties",
        "set {_config} to new slime world properties",
        "create slimeworld named \"Test\" with props (new slime world properties) with type %file%"
    ]
)
@Since("1.0.0")
class ExprCreateSlimeProperties : SimpleExpression<SlimePropertyMap>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprCreateSlimeProperties::class.java,
                SlimePropertyMap::class.java,
                ExpressionType.SIMPLE,
                "[default|new] slime world properties"
            )
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "Create Slime Properties Map"
    }

    override fun init(
        expressions: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parser: SkriptParser.ParseResult
    ): Boolean {
        return true
    }

    override fun isSingle(): Boolean {
        return true
    }

    override fun getReturnType(): Class<SlimePropertyMap> {
        return SlimePropertyMap::class.java
    }

    @Suppress("unchecked_cast")
    override fun get(event: Event): Array<SlimePropertyMap> {
        val propertyMap = SlimePropertyMap()

        SlimePropertiesEnum.entries.forEach { propertyEnum ->
            when (propertyEnum.dataType) {
                "String" -> {
                    val prop = propertyEnum.prop as com.infernalsuite.asp.api.world.properties.SlimeProperty<String, *>
                    propertyMap.setValue(prop, prop.defaultValue)
                }

                "Integer" -> {
                    val prop = propertyEnum.prop as com.infernalsuite.asp.api.world.properties.SlimeProperty<Int, *>
                    propertyMap.setValue(prop, prop.defaultValue)
                }

                "Float" -> {
                    val prop = propertyEnum.prop as com.infernalsuite.asp.api.world.properties.SlimeProperty<Float, *>
                    propertyMap.setValue(prop, prop.defaultValue)
                }

                "Boolean" -> {
                    val prop = propertyEnum.prop as com.infernalsuite.asp.api.world.properties.SlimeProperty<Boolean, *>
                    propertyMap.setValue(prop, prop.defaultValue)
                }
            }
        }

        return arrayOf(propertyMap)
    }
}
