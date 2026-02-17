package dev.danielmillar.slimelink.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class SourcesConfig(
    @Comment("Settings related to the File loader.")
    val file: FileConfig = FileConfig(),
    @Comment("Settings related to the MySQL loader.")
    val mysql: MySQLConfig = MySQLConfig(),
    @Comment("Settings related to the MongoDB loader.")
    val mongodb: MongoDBConfig = MongoDBConfig()
) {

    @ConfigSerializable
    data class FileConfig(
        val path: String = "slime_worlds"
    )

    @ConfigSerializable
    data class MySQLConfig(
        val enabled: Boolean = false,
        val host: String = "127.0.0.1",
        val port: Int = 3306,
        val username: String = "slimeworldmanager",
        val password: String = "",
        val database: String = "slimeworldmanager",
        val useSsl: Boolean = false
    ) {
        val url: String
            get() = "jdbc:mysql://$host:$port/$database?autoReconnect=true&allowMultiQueries=true&useSSL=$useSsl"
    }

    @ConfigSerializable
    data class MongoDBConfig(
        val enabled: Boolean = false,
        val host: String = "127.0.0.1",
        val port: Int = 27017,
        val authSource: String = "admin",
        val username: String = "slimeworldmanager",
        val password: String = "",
        val database: String = "slimeworldmanager",
        val collection: String = "worlds",
        val uri: String = ""
    )
}
