package dev.danielmillar.slimelink.platform

enum class ServerPlatform(private val detectClasses: List<String>) {
    PURPUR(listOf("org.purpurmc.purpur.PurpurConfig")),
    PAPER(
        listOf(
            "com.destroystokyo.paper.PaperConfig",
            "io.papermc.paper.plugin.PaperPlugin"
        )
    ),
    UNKNOWN(emptyList());

    companion object {
        fun detect(): ServerPlatform {
            for (platform in entries) {
                for (cls in platform.detectClasses) {
                    if (isPresent(cls)) {
                        return platform
                    }
                }
            }
            return UNKNOWN
        }

        private fun isPresent(className: String): Boolean =
            try {
                Class.forName(className)
                true
            } catch (_: ClassNotFoundException) {
                false
            }
    }
}