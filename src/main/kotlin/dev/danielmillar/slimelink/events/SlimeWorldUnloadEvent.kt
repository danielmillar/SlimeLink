package dev.danielmillar.slimelink.events

import org.bukkit.World
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SlimeWorldUnloadEvent(
    val slimeworld: World
) : Event(), Cancellable {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }

    private var cancelled = false

    override fun getHandlers(): HandlerList = HANDLER_LIST

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}