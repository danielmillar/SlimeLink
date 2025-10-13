package dev.danielmillar.slimelink

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.skript.Types
import org.bukkit.plugin.java.JavaPlugin

class SlimeLink : JavaPlugin() {

    companion object {
        private lateinit var instance: SlimeLink
        fun getInstance(): SlimeLink {
            return instance
        }

        private lateinit var ASP: AdvancedSlimePaperAPI
        fun getASP(): AdvancedSlimePaperAPI {
            return ASP
        }
    }

    private lateinit var metrics: Metrics
    private lateinit var addon: SkriptAddon

    override fun onEnable() {
        instance = this

        try {
            Class.forName("com.infernalsuite.asp.AdvancedSlimePaper")
            ASP = AdvancedSlimePaperAPI.instance()
        } catch (_: ClassNotFoundException) {
            slF4JLogger.error("AdvancedSlimePaper is not installed! Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        metrics = Metrics(this, 27582)

        try {
            ConfigManager.initialize()
        } catch (ex: Exception) {
            slF4JLogger.error("Failed to load config files", ex)
        }

        addon = Skript.registerAddon(this).setLanguageFileDirectory("lang")
        try {
            Types()
            addon.loadClasses("dev.danielmillar.slimelink")
        } catch (ex: Exception) {
            slF4JLogger.error("Failed to load Skript classes", ex)
        }
    }

    override fun onDisable() {
        metrics.shutdown()
    }
}
