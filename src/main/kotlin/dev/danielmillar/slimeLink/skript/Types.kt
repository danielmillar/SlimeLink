package dev.danielmillar.slimeLink.skript

import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.classes.EnumClassInfo
import ch.njol.skript.classes.Parser
import ch.njol.skript.classes.Serializer
import ch.njol.skript.lang.ParseContext
import ch.njol.skript.registrations.Classes
import ch.njol.yggdrasil.Fields
import com.infernalsuite.asp.api.world.properties.SlimeProperties
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import dev.danielmillar.slimeLink.slime.SlimeLoaderTypeEnum
import dev.danielmillar.slimeLink.slime.SlimePropertiesEnum

class Types {

    companion object {
        init {
            Classes.registerClass(
                EnumClassInfo(SlimeLoaderTypeEnum::class.java, "slimeloader", "slime loaders")
                    .user("loaders")
                    .name("Loader")
                    .description("Represents a Slime loader.")
                    .since("1.0.0")
            )

            Classes.registerClass(
                EnumClassInfo(SlimePropertiesEnum::class.java, "slimeproperty", "slime properties")
                    .user("properties")
                    .since("1.0.0")
            )

            Classes.registerClass(
                ClassInfo(SlimePropertyMap::class.java, "slimepropertymap")
                    .since("1.0.0")
                    .parser(object : Parser<SlimePropertyMap>() {
                        override fun canParse(context: ParseContext?): Boolean {
                            return false
                        }

                        override fun toString(slimePropertyMap: SlimePropertyMap?, flags: Int): String {
                            return toVariableNameString(slimePropertyMap)
                        }

                        override fun toVariableNameString(slimePropertyMap: SlimePropertyMap?): String {
                            return slimePropertyMap?.let {
                                "SlimePropertyMap{" +
                                        "spawnX=${it.getValue(SlimeProperties.SPAWN_X)}," +
                                        "spawnY=${it.getValue(SlimeProperties.SPAWN_Y)}," +
                                        "spawnZ=${it.getValue(SlimeProperties.SPAWN_Z)}," +
                                        "spawnYaw=${it.getValue(SlimeProperties.SPAWN_YAW)}," +
                                        "difficulty=${it.getValue(SlimeProperties.DIFFICULTY)}," +
                                        "allowMonsters=${it.getValue(SlimeProperties.ALLOW_MONSTERS)}," +
                                        "allowAnimals=${it.getValue(SlimeProperties.ALLOW_ANIMALS)}," +
                                        "dragonBattle=${it.getValue(SlimeProperties.DRAGON_BATTLE)}," +
                                        "pvp=${it.getValue(SlimeProperties.PVP)}," +
                                        "environment=${it.getValue(SlimeProperties.ENVIRONMENT)}," +
                                        "worldType=${it.getValue(SlimeProperties.WORLD_TYPE)}," +
                                        "defaultBiome=${it.getValue(SlimeProperties.DEFAULT_BIOME)}" +
                                        "}"
                            } ?: "SlimePropertyMap is null"
                        }
                    })
                    .serializer(object : Serializer<SlimePropertyMap>() {
                        override fun serialize(slimePropertyMap: SlimePropertyMap?): Fields {
                            val fields = Fields()
                            slimePropertyMap?.let {
                                fields.putPrimitive("spawnX", slimePropertyMap.getValue(SlimeProperties.SPAWN_X))
                                fields.putPrimitive("spawnY", slimePropertyMap.getValue(SlimeProperties.SPAWN_Y))
                                fields.putPrimitive("spawnZ", slimePropertyMap.getValue(SlimeProperties.SPAWN_Z))
                                fields.putPrimitive("spawnYaw", slimePropertyMap.getValue(SlimeProperties.SPAWN_YAW))
                                fields.putObject("difficulty", slimePropertyMap.getValue(SlimeProperties.DIFFICULTY))
                                fields.putPrimitive(
                                    "allowMonsters",
                                    slimePropertyMap.getValue(SlimeProperties.ALLOW_MONSTERS)
                                )
                                fields.putPrimitive(
                                    "allowAnimals",
                                    slimePropertyMap.getValue(SlimeProperties.ALLOW_ANIMALS)
                                )
                                fields.putPrimitive(
                                    "dragonBattle",
                                    slimePropertyMap.getValue(SlimeProperties.DRAGON_BATTLE)
                                )
                                fields.putPrimitive("pvp", slimePropertyMap.getValue(SlimeProperties.PVP))
                                fields.putObject(
                                    "environment",
                                    slimePropertyMap.getValue(SlimeProperties.ENVIRONMENT)
                                )
                                fields.putObject("worldType", slimePropertyMap.getValue(SlimeProperties.WORLD_TYPE))
                                fields.putObject(
                                    "defaultBiome",
                                    slimePropertyMap.getValue(SlimeProperties.DEFAULT_BIOME)
                                )
                            }
                            return fields
                        }

                        override fun canBeInstantiated(): Boolean {
                            return false
                        }

                        override fun mustSyncDeserialization(): Boolean {
                            return true
                        }

                        override fun deserialize(slimePropertyMap: SlimePropertyMap?, fields: Fields?) {
                            assert(false)
                        }

                        override fun deserialize(fields: Fields?): SlimePropertyMap {
                            val slimePropertyMap = SlimePropertyMap()

                            fields?.let {
                                slimePropertyMap.setValue(
                                    SlimeProperties.SPAWN_X,
                                    it.getPrimitive("spawnX", Int::class.java) ?: SlimeProperties.SPAWN_X.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.SPAWN_Y,
                                    it.getPrimitive("spawnY", Int::class.java) ?: SlimeProperties.SPAWN_Y.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.SPAWN_Z,
                                    it.getPrimitive("spawnZ", Int::class.java) ?: SlimeProperties.SPAWN_Z.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.SPAWN_YAW,
                                    it.getPrimitive("spawnYaw", Float::class.java)
                                        ?: SlimeProperties.SPAWN_YAW.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.DIFFICULTY,
                                    it.getObject("difficulty", String::class.java)
                                        ?: SlimeProperties.DIFFICULTY.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.ALLOW_MONSTERS,
                                    it.getPrimitive("allowMonsters", Boolean::class.java)
                                        ?: SlimeProperties.ALLOW_MONSTERS.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.ALLOW_ANIMALS,
                                    it.getPrimitive("allowAnimals", Boolean::class.java)
                                        ?: SlimeProperties.ALLOW_ANIMALS.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.DRAGON_BATTLE,
                                    it.getPrimitive("dragonBattle", Boolean::class.java)
                                        ?: SlimeProperties.DRAGON_BATTLE.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.PVP,
                                    it.getPrimitive("pvp", Boolean::class.java) ?: SlimeProperties.PVP.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.ENVIRONMENT,
                                    it.getObject("environment", String::class.java)
                                        ?: SlimeProperties.ENVIRONMENT.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.WORLD_TYPE,
                                    it.getObject("worldType", String::class.java)
                                        ?: SlimeProperties.WORLD_TYPE.defaultValue
                                )
                                slimePropertyMap.setValue(
                                    SlimeProperties.DEFAULT_BIOME,
                                    it.getObject("defaultBiome", String::class.java)
                                        ?: SlimeProperties.DEFAULT_BIOME.defaultValue
                                )
                            }
                            return slimePropertyMap
                        }

                    })
            )
        }
    }
}