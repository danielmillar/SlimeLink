package dev.danielmillar.slimelink.events

import com.infernalsuite.asp.api.world.SlimeWorld
import org.bukkit.World
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SlimeWorldUnloadEvent(
    val world: World,
    val slimeWorld: SlimeWorld
) : Event(false) {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmField
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
