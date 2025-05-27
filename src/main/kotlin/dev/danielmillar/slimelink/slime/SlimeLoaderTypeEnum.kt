package dev.danielmillar.slimelink.slime

enum class SlimeLoaderTypeEnum(val loaderId: String) {
    FILE("file"),
    MYSQL("mysql"),
    MONGODB("mongodb");

    companion object {
        fun fromId(loaderId: String): SlimeLoaderTypeEnum? {
            return entries.find { it.loaderId.equals(loaderId, ignoreCase = true) }
        }
    }
}