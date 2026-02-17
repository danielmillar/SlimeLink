package dev.danielmillar.slimelink.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Manages configuration files with support for reactive state flows.
 *
 * @param dataFolder The folder where configuration files are stored
 * @param logger The provided logger which is used to log anything
 */
class ConfigManager(
    private val dataFolder: Path,
    private val logger: org.slf4j.Logger
) {

    private val configs = ConcurrentHashMap<KClass<*>, ConfigEntry<*>>()

    /**
     * Registers and loads a configuration file.
     *
     * @param fileName The name of the config file (e.g., "config.yml")
     * @param defaultProvider Provides default config instance if file doesn't exist or is invalid
     * @return The loaded or default configuration instance
     */
    inline fun <reified T : Any> register(
        fileName: String,
        noinline defaultProvider: () -> T
    ): T = register(T::class, fileName, defaultProvider)

    /**
     * Registers and loads a configuration file.
     *
     * @param clazz The KClass of the config type
     * @param fileName The name of the config file (e.g., "config.yml")
     * @param defaultProvider Provides default config instance if file doesn't exist or is invalid
     * @return The loaded or default configuration instance
     */
    @PublishedApi
    internal fun <T : Any> register(
        clazz: KClass<T>,
        fileName: String,
        defaultProvider: () -> T
    ): T {
        val loader = createLoader(fileName)
        val config = loadOrCreate(loader, clazz, defaultProvider)
        val entry = ConfigEntry(loader, MutableStateFlow(config))

        val existing = configs.putIfAbsent(clazz, entry)
        require(existing == null) { "Config type ${clazz.simpleName} is already registered" }

        return config
    }

    /**
     * Gets the configuration instance for the specified type.
     */
    inline fun <reified T : Any> get(): T = get(T::class)

    /**
     * Gets the configuration instance for the specified type.
     */
    fun <T : Any> get(clazz: KClass<T>): T {
        return getEntry(clazz).flow.value
    }

    /**
     * Gets a reactive state flow for the configuration.
     * Emits new values when [reload] or [update] is called.
     */
    inline fun <reified T : Any> stateFlow(): StateFlow<T> = stateFlow(T::class)

    /**
     * Gets a reactive state flow for the configuration.
     * Emits new values when [reload] or [update] is called.
     */
    fun <T : Any> stateFlow(clazz: KClass<T>): StateFlow<T> {
        return getEntry(clazz).flow.asStateFlow()
    }

    /**
     * Updates the in-memory configuration and notifies subscribers.
     * Call [save] to persist changes to disk.
     */
    inline fun <reified T : Any> update(config: T) = update(T::class, config)

    /**
     * Updates the in-memory configuration and notifies subscribers.
     * Call [save] to persist changes to disk.
     */
    fun <T : Any> update(clazz: KClass<T>, config: T) {
        val entry = getEntry(clazz)
        entry.flow.value = config
    }

    /**
     * Saves the configuration to disk.
     */
    inline fun <reified T : Any> save(): Boolean = save(T::class)

    /**
     * Saves the configuration to disk.
     */
    fun <T : Any> save(clazz: KClass<T>): Boolean {
        val entry = getEntry(clazz)
        return runCatching {
            val node = entry.loader.createNode()
            node.set(entry.flow.value)
            entry.loader.save(node)
            logger.info("Saved config: {}", clazz.simpleName)
        }.onFailure { e ->
            logger.warn("Failed to save config {}: {}", clazz.simpleName, e.cause?.stackTrace)
        }.isSuccess
    }

    /**
     * Saves all registered configurations to disk.
     */
    fun saveAll() {
        configs.keys.forEach { save(it) }
    }

    /**
     * Reloads the configuration from disk.
     * @return The reloaded configuration, or null if reload failed
     */
    inline fun <reified T : Any> reload(): T? = reload(T::class)

    /**
     * Reloads the configuration from disk.
     * @return The reloaded configuration, or null if reload failed
     */
    fun <T : Any> reload(clazz: KClass<T>): T? {
        val entry = getEntry(clazz)

        return runCatching {
            val node = entry.loader.load()
            node.get(clazz.java)
                ?: error("Config file is empty or invalid for ${clazz.simpleName}")
        }.onSuccess { config ->
            entry.flow.value = config
            logger.info("Reloaded config: {}", clazz.simpleName)
        }.onFailure { e ->
            logger.warn("Failed to reload config {}: {}", clazz.simpleName, e.cause?.stackTrace)
        }.getOrNull()
    }

    /**
     * Reloads all registered configurations from disk.
     */
    fun reloadAll(): Result<Pair<Int, Int>> {
        if (configs.isEmpty()) {
            logger.info("No configs to reload")
            return Result.success(0 to 0)
        }

        var successCount = 0
        configs.keys.forEach { clazz ->
            val result = runCatching { reload(clazz) }
            if (result.isSuccess && result.getOrNull() != null) {
                successCount++
            }
        }
        logger.info("Reloaded $successCount/${configs.size} configs")
        return Result.success(successCount to configs.size)
    }

    private fun createLoader(fileName: String): ConfigurationLoader<CommentedConfigurationNode> {
        return YamlConfigurationLoader.builder()
            .path(dataFolder.resolve(fileName))
            .nodeStyle(NodeStyle.BLOCK)
            .commentsEnabled(true)
            .lineLength(1000)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(
                        ObjectMapper.factoryBuilder()
                            .defaultNamingScheme(NamingSchemes.CAMEL_CASE)
                            .build()
                    )
                }
            }
            .build()
    }

    private fun <T : Any> loadOrCreate(
        loader: ConfigurationLoader<CommentedConfigurationNode>,
        clazz: KClass<T>,
        defaultProvider: () -> T
    ): T {
        return runCatching {
            val node = loader.load()
            node.get(clazz.java) ?: defaultProvider().also { default ->
                node.set(default)
                loader.save(node)
                logger.info("Created default config: {}", clazz.simpleName)
            }
        }.getOrElse { e ->
            when (e) {
                is ConfigurateException -> {
                    logger.warn("Failed to load config {}, using defaults: {}", clazz.simpleName, e.cause?.stackTrace)
                    defaultProvider()
                }
                else -> throw e
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getEntry(clazz: KClass<T>): ConfigEntry<T> {
        return configs[clazz] as? ConfigEntry<T>
            ?: error("Config type ${clazz.simpleName} is not registered")
    }

    private class ConfigEntry<T : Any>(
        val loader: ConfigurationLoader<CommentedConfigurationNode>,
        val flow: MutableStateFlow<T>
    )
}