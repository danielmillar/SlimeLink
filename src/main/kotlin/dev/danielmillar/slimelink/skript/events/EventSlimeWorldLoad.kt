package dev.danielmillar.slimelink.skript.events

import ch.njol.skript.Skript
import ch.njol.skript.lang.util.SimpleEvent
import ch.njol.skript.registrations.EventValues
import dev.danielmillar.slimelink.events.SlimeWorldLoadEvent
import org.bukkit.World

class EventSlimeWorldLoad {

    companion object {
        init {
            Skript.registerEvent(
                "SlimeWorldLoadEvent",
                SimpleEvent::class.java,
                SlimeWorldLoadEvent::class.java,
                "(slimeworld|slime world) load"
            )

            EventValues.registerEventValue(SlimeWorldLoadEvent::class.java, World::class.java, SlimeWorldLoadEvent::slimeworld)
        }
    }
}