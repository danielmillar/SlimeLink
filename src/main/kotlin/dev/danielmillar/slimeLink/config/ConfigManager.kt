package dev.danielmillar.slimeLink.config

import dev.danielmillar.slimeLink.SlimeLink
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.loader.HeaderMode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ConfigManager {

    private val pluginDir = File("plugins", "SlimeLink").apply { mkdirs() }
    private val worldsFile = File(pluginDir, "worlds.yml")
    private val sourcesFile = File(pluginDir, "sources.yml")

    private lateinit var worldLoader: YamlConfigurationLoader
    private lateinit var datasourcesLoader: YamlConfigurationLoader

    private lateinit var worldConfig: WorldsConfig
    private lateinit var datasourcesConfig: DatasourcesConfig

    @Throws(IOException::class)
    fun initialize() {
        copyDefaultConfigs()

        worldLoader = YamlConfigurationLoader.builder()
            .file(worldsFile)
            .nodeStyle(NodeStyle.BLOCK)
            .headerMode(HeaderMode.PRESERVE)
            .build()

        worldConfig = worldLoader.load().get(TypeToken.get(WorldsConfig::class.java))
            ?: WorldsConfig()

        datasourcesLoader = YamlConfigurationLoader.builder()
            .file(sourcesFile)
            .nodeStyle(NodeStyle.BLOCK)
            .headerMode(HeaderMode.PRESERVE)
            .build()

        datasourcesConfig = datasourcesLoader.load().get(TypeToken.get(DatasourcesConfig::class.java))
            ?: DatasourcesConfig()

        saveWorldConfig()
        saveDatasourcesConfig()
    }

    private fun copyDefaultConfigs() {
        if (!worldsFile.exists()) {
            SlimeLink.getInstance().getResource("worlds.yml")?.use { input ->
                Files.copy(input, worldsFile.toPath())
            }
        }
        if (!sourcesFile.exists()) {
            SlimeLink.getInstance().getResource("sources.yml")?.use { input ->
                Files.copy(input, sourcesFile.toPath())
            }
        }
    }

    @Throws(IOException::class)
    fun saveWorldConfig() {
        worldLoader.save(
            worldLoader.createNode()
                .set(TypeToken.get(WorldsConfig::class.java), worldConfig)
        )
    }

    @Throws(IOException::class)
    fun saveDatasourcesConfig() {
        datasourcesLoader.save(
            datasourcesLoader.createNode()
                .set(TypeToken.get(DatasourcesConfig::class.java), datasourcesConfig)
        )
    }

    fun getWorldConfig(): WorldsConfig = worldConfig
    fun getDatasourcesConfig(): DatasourcesConfig = datasourcesConfig
}
