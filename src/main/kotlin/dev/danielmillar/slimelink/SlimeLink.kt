package dev.danielmillar.slimelink

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI
import dev.danielmillar.slimelink.config.ConfigManager
import dev.danielmillar.slimelink.platform.ServerPlatform
import dev.danielmillar.slimelink.skript.Types
import org.bukkit.plugin.java.JavaPlugin

class SlimeLink : JavaPlugin() {

    companion object {
        private lateinit var instance: SlimeLink
        fun getInstance(): SlimeLink {
            return instance
        }

        private val ASP: AdvancedSlimePaperAPI = AdvancedSlimePaperAPI.instance()

        fun getASP(): AdvancedSlimePaperAPI {
            return ASP
        }
    }

    private lateinit var addon: SkriptAddon
    lateinit var platform: ServerPlatform
        private set

    override fun onEnable() {
        instance = this

        platform = ServerPlatform.detect()
        logger.info { "Platform Detected: We're running ${platform.name}" }

        try {
            ConfigManager.initialize()
        } catch (ex: Exception) {
            slF4JLogger.error("Failed to load config files", ex)
        }

        addon = Skript.registerAddon(this).setLanguageFileDirectory("lang")
        try {
            Types()
            addon.loadClasses("dev.danielmillar.slimeLink")
        } catch (ex: Exception) {
            slF4JLogger.error("Failed to load Skript classes", ex)
        }
    }

    override fun onDisable() {}
}
