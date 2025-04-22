package dev.danielmillar.slimeLink.config

import com.infernalsuite.asp.api.world.properties.SlimeProperties.*
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap
import org.bukkit.Difficulty
import org.bukkit.World
import org.bukkit.WorldType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class WorldData(
    @Setting("source")
    private val source: String = "file",

    @Setting("spawn")
    private val spawn: List<Double> = listOf(0.5, 255.0, 0.5),

    @Setting("difficulty")
    private val difficulty: Difficulty = Difficulty.PEACEFUL,

    @Setting("allowMonsters")
    private val allowMonsters: Boolean = true,

    @Setting("allowAnimals")
    private val allowAnimals: Boolean = true,

    @Setting("dragonBattle")
    private val dragonBattle: Boolean = false,

    @Setting("pvp")
    private val pvp: Boolean = true,

    @Setting("environment")
    private val environment: World.Environment = World.Environment.NORMAL,

    @Setting("worldType")
    private val worldType: WorldType = WorldType.NORMAL,

    @Setting("defaultBiome")
    private val defaultBiome: String = "minecraft:plains",

    @Setting("readOnly")
    private val readOnly: Boolean = false
) {
    fun getSource(): String = source
    fun getSpawn(): List<Double> = spawn
    fun getDifficulty(): Difficulty = difficulty
    fun isAllowMonsters(): Boolean = allowMonsters
    fun isAllowAnimals(): Boolean = allowAnimals
    fun isDragonBattle(): Boolean = dragonBattle
    fun isPvp(): Boolean = pvp
    fun getEnvironment(): World.Environment = environment
    fun getWorldType(): WorldType = worldType
    fun getDefaultBiome(): String = defaultBiome
    fun isReadOnly(): Boolean = readOnly
    fun toPropertyMap(): SlimePropertyMap {
        return SlimePropertyMap().apply {
            setValue(SPAWN_X, spawn.getOrNull(0)?.toInt() ?: 0)
            setValue(SPAWN_Y, spawn.getOrNull(1)?.toInt() ?: 0)
            setValue(SPAWN_Z, spawn.getOrNull(2)?.toInt() ?: 0)
            setValue(DIFFICULTY, difficulty.name.lowercase())
            setValue(ALLOW_MONSTERS, allowMonsters)
            setValue(ALLOW_ANIMALS, allowAnimals)
            setValue(DRAGON_BATTLE, dragonBattle)
            setValue(PVP, pvp)
            setValue(ENVIRONMENT, environment.name)
            setValue(WORLD_TYPE, worldType.name)
            setValue(DEFAULT_BIOME, defaultBiome)
        }
    }
}
