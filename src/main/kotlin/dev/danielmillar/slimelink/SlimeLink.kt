package dev.danielmillar.slimelink

import Metrics
import ch.njol.skript.Skript
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.config.SourcesConfig
import dev.danielmillar.slimelink.slime.SlimeLoaderType
import dev.danielmillar.slimelink.skript.Types
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.addon.SkriptAddon
import org.skriptlang.skript.util.ClassLoader as SkriptClassLoader

class SlimeLink : JavaPlugin() {

    companion object {
        lateinit var instance: SlimeLink
            private set

        val asp: AdvancedSlimePaperAPI
            get() = instance._asp ?: error("ASP not initialized")

        val skriptAddon: SkriptAddon
            get() = instance.addon
    }

    private var _asp: AdvancedSlimePaperAPI? = null

    lateinit var configManager: ConfigManager
        private set

    private lateinit var metrics: Metrics
    private lateinit var addon: SkriptAddon

    override fun onEnable() {
        instance = this

        val aspClass = try {
            Class.forName("com.infernalsuite.asp.AdvancedSlimePaper")
            true
        } catch (_: ClassNotFoundException) {
            false
        }

        if (!aspClass) {
            slF4JLogger.error("AdvancedSlimePaper is not installed! Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }
        _asp = AdvancedSlimePaperAPI.instance()

        configManager = ConfigManager(this.dataPath, slF4JLogger).apply {
            register<SourcesConfig>("sources.yml",) {
                SourcesConfig()
            }
        }
        configManager.saveAll()

        metrics = Metrics(this, 27582)

        addon = Skript.instance().registerAddon(this::class.java, name)
        addon.localizer().setSourceDirectories("lang", dataFolder.resolve("lang").absolutePath)
        runCatching {
            Types()
            SkriptClassLoader.builder()
                .basePackage("dev.danielmillar.slimelink")
                .initialize(true)
                .deep(true)
                .build()
                .loadClasses(this::class.java)
        }.onFailure {
            slF4JLogger.error("Failed to load classes required for Skript, Disabling plugin.", it)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        SlimeLoaderType.clearCache()
        if (::metrics.isInitialized) {
            metrics.shutdown()
        }
    }
}
