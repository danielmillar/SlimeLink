package dev.danielmillar.slimelink.slime

import com.infernalsuite.asp.api.world.properties.SlimeProperties
import com.infernalsuite.asp.api.world.properties.SlimeProperty

enum class SlimePropertiesEnum(val prop: SlimeProperty<*, *>, val dataType: String) {
    SPAWN_X(SlimeProperties.SPAWN_X, "Integer"),
    SPAWN_Y(SlimeProperties.SPAWN_Y, "Integer"),
    SPAWN_Z(SlimeProperties.SPAWN_Z, "Integer"),
    SPAWN_YAW(SlimeProperties.SPAWN_YAW, "Float"),
    DIFFICULTY(SlimeProperties.DIFFICULTY, "String"),
    ALLOW_MONSTERS(SlimeProperties.ALLOW_MONSTERS, "Boolean"),
    ALLOW_ANIMALS(SlimeProperties.ALLOW_ANIMALS, "Boolean"),
    DRAGON_BATTLE(SlimeProperties.DRAGON_BATTLE, "Boolean"),
    PVP(SlimeProperties.PVP, "Boolean"),
    ENVIRONMENT(SlimeProperties.ENVIRONMENT, "String"),
    WORLD_TYPE(SlimeProperties.WORLD_TYPE, "String"),
    DEFAULT_BIOME(SlimeProperties.DEFAULT_BIOME, "String")
}
