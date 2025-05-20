package dev.danielmillar.slimelink.slime

enum class SlimeLoaderTypeEnum(val id: String) {
    FILE("file"),
    MYSQL("mysql"),
    MONGODB("mongodb");

    companion object {
        fun fromId(id: String): SlimeLoaderTypeEnum? {
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}