package dev.danielmillar.slimelink.skript.events

import ch.njol.skript.Skript
import ch.njol.skript.lang.util.SimpleEvent
import ch.njol.skript.registrations.EventValues
import dev.danielmillar.slimelink.events.SlimeWorldUnloadEvent
import org.bukkit.World

class EventSlimeWorldUnload {

    companion object {
        init {
            Skript.registerEvent(
                "SlimeWorldUnloadEvent",
                SimpleEvent::class.java,
                SlimeWorldUnloadEvent::class.java,
                "(slimeworld|slime world) unload"
            )

            EventValues.registerEventValue(SlimeWorldUnloadEvent::class.java, World::class.java, SlimeWorldUnloadEvent::slimeworld)
        }
    }
}