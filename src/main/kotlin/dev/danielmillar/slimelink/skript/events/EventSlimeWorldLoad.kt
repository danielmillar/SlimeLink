package dev.danielmillar.slimelink.skript.events

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.util.SimpleEvent
import ch.njol.skript.registrations.EventValues
import dev.danielmillar.slimelink.events.SlimeWorldLoadEvent
import org.bukkit.World

@Name("SlimeWorld Load Event")
@Description("Triggered when a SlimeWorld is loaded.")
@Examples(
    value = [
        "on slime world load:",
        "\t# Code to execute when a SlimeWorld is loaded"
    ]
)
@Since("1.0.0")
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