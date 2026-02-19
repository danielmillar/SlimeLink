package dev.danielmillar.slimelink.slime

import com.infernalsuite.asp.api.world.properties.SlimeProperties
import com.infernalsuite.asp.api.world.properties.SlimeProperty
import java.util.Locale

enum class SlimePropertiesEnum(val prop: SlimeProperty<*, *>) {
    SPAWN_X(SlimeProperties.SPAWN_X),
    SPAWN_Y(SlimeProperties.SPAWN_Y),
    SPAWN_Z(SlimeProperties.SPAWN_Z),
    SPAWN_YAW(SlimeProperties.SPAWN_YAW),
    DIFFICULTY(SlimeProperties.DIFFICULTY),
    ALLOW_MONSTERS(SlimeProperties.ALLOW_MONSTERS),
    ALLOW_ANIMALS(SlimeProperties.ALLOW_ANIMALS),
    DRAGON_BATTLE(SlimeProperties.DRAGON_BATTLE),
    PVP(SlimeProperties.PVP),
    ENVIRONMENT(SlimeProperties.ENVIRONMENT),
    WORLD_TYPE(SlimeProperties.WORLD_TYPE),
    DEFAULT_BIOME(SlimeProperties.DEFAULT_BIOME);

    fun expectedTypeName(): String {
        val type = prop.defaultValue?.javaClass ?: Any::class.java
        return when (type) {
            Int::class.javaObjectType -> "Integer"
            Float::class.javaObjectType -> "Float"
            Boolean::class.javaObjectType -> "Boolean"
            String::class.java -> "String"
            else -> type.simpleName
        }
    }

    fun coerce(rawValue: Any): Any? {
        return when (prop.defaultValue) {
            is Int -> coerceToInt(rawValue)
            is Float -> coerceToFloat(rawValue)
            is Boolean -> coerceToBoolean(rawValue)
            is String -> rawValue.toString()
            null -> rawValue
            else -> if ((prop.defaultValue as Any)::class.java.isInstance(rawValue)) rawValue else null
        }
    }

    private fun coerceToInt(value: Any): Int? {
        return when (value) {
            is Int -> value
            is Byte, is Short, is Long -> {
                val longValue = (value as Number).toLong()
                if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) longValue.toInt() else null
            }
            is Float, is Double -> {
                val doubleValue = (value as Number).toDouble()
                if (!doubleValue.isFinite()) return null
                if (doubleValue % 1.0 != 0.0) return null
                if (doubleValue < Int.MIN_VALUE || doubleValue > Int.MAX_VALUE) return null
                doubleValue.toInt()
            }
            is Number -> {
                val doubleValue = value.toDouble()
                if (!doubleValue.isFinite()) return null
                if (doubleValue % 1.0 != 0.0) return null
                if (doubleValue < Int.MIN_VALUE || doubleValue > Int.MAX_VALUE) return null
                doubleValue.toInt()
            }
            is String -> value.trim().toIntOrNull()
            else -> null
        }
    }

    private fun coerceToFloat(value: Any): Float? {
        return when (value) {
            is Float -> if (value.isFinite()) value else null
            is Number -> {
                val doubleValue = value.toDouble()
                if (!doubleValue.isFinite()) return null
                if (doubleValue < -Float.MAX_VALUE || doubleValue > Float.MAX_VALUE) return null
                doubleValue.toFloat()
            }
            is String -> value.trim().toFloatOrNull()
            else -> null
        }
    }

    private fun coerceToBoolean(value: Any): Boolean? {
        return when (value) {
            is Boolean -> value
            is Number -> {
                val doubleValue = value.toDouble()
                when {
                    !doubleValue.isFinite() -> null
                    doubleValue == 0.0 -> false
                    doubleValue == 1.0 -> true
                    else -> null
                }
            }
            is String -> {
                when (value.trim().lowercase(Locale.ROOT)) {
                    "true", "yes", "on", "1" -> true
                    "false", "no", "off", "0" -> false
                    else -> null
                }
            }
            else -> null
        }
    }
}
