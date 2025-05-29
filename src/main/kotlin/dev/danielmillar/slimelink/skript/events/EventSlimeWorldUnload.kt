package dev.danielmillar.slimelink.skript.events

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.util.SimpleEvent
import ch.njol.skript.registrations.EventValues
import dev.danielmillar.slimelink.events.SlimeWorldUnloadEvent
import org.bukkit.World

@Name("SlimeWorld Unload Event")
@Description("Triggered when a SlimeWorld is unloaded.")
@Examples(
    value = [
        "on slime world unload:",
        "\t# Code to execute when a SlimeWorld is unloaded"
    ]
)
@Since("1.0.0")
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