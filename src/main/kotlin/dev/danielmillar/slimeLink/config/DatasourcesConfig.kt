package dev.danielmillar.slimeLink.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class DatasourcesConfig(
    @Setting("file")
    private val file: FileConfig = FileConfig(),

    @Setting("mysql")
    private val mysql: MysqlConfig = MysqlConfig(),

    @Setting("mongodb")
    private val mongodb: MongoDbConfig = MongoDbConfig()
) {
    fun getFileConfig(): FileConfig = file
    fun getMysqlConfig(): MysqlConfig = mysql
    fun getMongoDbConfig(): MongoDbConfig = mongodb

    @ConfigSerializable
    data class FileConfig(
        @Setting("path")
        private val path: String = "slime_worlds"
    ) {
        fun getPath(): String = path
    }

    @ConfigSerializable
    data class MysqlConfig(
        @Setting("enabled")
        private val enabled: Boolean = false,

        @Setting("host")
        private val host: String = "127.0.0.1",

        @Setting("port")
        private val port: Int = 3306,

        @Setting("username")
        private val username: String = "slimeworldmanager",

        @Setting("password")
        private val password: String = "",

        @Setting("database")
        private val database: String = "slimeworldmanager",

        @Setting("useSsl")
        private val useSsl: Boolean = false
    ) {
        fun isEnabled(): Boolean = enabled
        fun getHost(): String = host
        fun getPort(): Int = port
        fun getUsername(): String = username
        fun getPassword(): String = password
        fun getDatabase(): String = database
        fun isUseSsl(): Boolean = useSsl
        val url: String
            get() = "jdbc:mysql://$host:$port/$database?autoReconnect=true&allowMultiQueries=true&useSSL=$useSsl"
    }

    @ConfigSerializable
    data class MongoDbConfig(
        @Setting("enabled")
        private val enabled: Boolean = false,

        @Setting("host")
        private val host: String = "127.0.0.1",

        @Setting("port")
        private val port: Int = 27017,

        @Setting("authSource")
        private val authSource: String = "admin",

        @Setting("username")
        private val username: String = "slimeworldmanager",

        @Setting("password")
        private val password: String = "",

        @Setting("database")
        private val database: String = "slimeworldmanager",

        @Setting("collection")
        private val collection: String = "worlds",

        @Setting("uri")
        private val uri: String = ""
    ) {
        fun isEnabled(): Boolean = enabled
        fun getHost(): String = host
        fun getPort(): Int = port
        fun getAuthSource(): String = authSource
        fun getUsername(): String = username
        fun getPassword(): String = password
        fun getDatabase(): String = database
        fun getCollection(): String = collection
        fun getUri(): String = uri
        val connectionUri: String
            get() = uri.ifEmpty {
                val credentials = if (username.isNotEmpty()) {
                    "$username:${password}@"
                } else {
                    ""
                }
                "mongodb://$credentials$host:$port/$database?authSource=$authSource"
            }
    }
}
