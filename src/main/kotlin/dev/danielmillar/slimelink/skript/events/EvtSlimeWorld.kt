package dev.danielmillar.slimelink.skript.events

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.LiteralList
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.registrations.EventValues
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI
import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent
import com.infernalsuite.asp.api.world.SlimeWorld
import dev.danielmillar.slimelink.SlimeLink
import dev.danielmillar.slimelink.events.SlimeWorldUnloadEvent
import dev.danielmillar.slimelink.skript.registerEvent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldUnloadEvent
import java.util.concurrent.ConcurrentHashMap

@Name("SlimeWorld Load/Unload")
@Description(
    "Called when a Slime world is loaded by AdvancedSlimePaper.",
    "Called when a Slime world is unloaded from the server.",
    "Unlike Bukkit's generic world load/unload events, these only match Slime worlds."
)
@Examples(
    value = [
        "on slime world load:",
        "    broadcast \"Loaded Slime world: %event-world%\"",
        "",
        "on slime world unload:",
        "    broadcast \"Unloaded Slime world: %event-world%\"",
        "",
        "on slime world load of \"arena\":",
        "    set {last_slime_world} to event-world"
    ]
)
@Since("2.0.0")
class EvtSlimeWorld : SkriptEvent() {

    companion object {
        private val loadedSlimeWorlds: MutableMap<String, SlimeWorld> = ConcurrentHashMap()

        init {
            registerEvent(
                EvtSlimeWorld::class.java,
                "Slime World Load/Unload",
                arrayOf(LoadSlimeWorldEvent::class.java, SlimeWorldUnloadEvent::class.java),
                "slime world load[ing] [of %-strings%]",
                "slime world unload[ing] [of %-strings%]"
            ) { EvtSlimeWorld() }

            EventValues.registerEventValue(
                LoadSlimeWorldEvent::class.java,
                World::class.java
            ) { event -> event.slimeWorld.bukkitWorld }

            EventValues.registerEventValue(
                LoadSlimeWorldEvent::class.java,
                SlimeWorld::class.java
            ) { event -> event.slimeWorld }

            EventValues.registerEventValue(
                SlimeWorldUnloadEvent::class.java,
                World::class.java
            ) { event -> event.world }

            EventValues.registerEventValue(
                SlimeWorldUnloadEvent::class.java,
                SlimeWorld::class.java
            ) { event -> event.slimeWorld }

            runCatching {
                AdvancedSlimePaperAPI.instance().loadedWorlds.forEach { slimeWorld ->
                    loadedSlimeWorlds[slimeWorld.name] = slimeWorld
                }
            }

            Bukkit.getPluginManager().registerEvents(object : Listener {
                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                fun onSlimeWorldLoad(event: LoadSlimeWorldEvent) {
                    loadedSlimeWorlds[event.slimeWorld.name] = event.slimeWorld
                }

                @EventHandler(priority = EventPriority.MONITOR)
                fun onWorldUnload(event: WorldUnloadEvent) {
                    if (!event.isCancelled) {
                        val slimeWorld = loadedSlimeWorlds[event.world.name] ?: return
                        Bukkit.getPluginManager().callEvent(
                            SlimeWorldUnloadEvent(event.world, slimeWorld)
                        )
                        loadedSlimeWorlds.remove(event.world.name)
                    }
                }
            }, SlimeLink.instance)
        }

        fun clearState() {
            loadedSlimeWorlds.clear()
        }
    }

    private var worldNames: Literal<String>? = null
    private var unload = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        args: Array<out Literal<*>?>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        worldNames = args[0] as? Literal<String>
        if (worldNames is LiteralList<*> && worldNames!!.and) {
            (worldNames as LiteralList<String>).invertAnd()
        }
        unload = matchedPattern == 1
        return true
    }

    override fun check(event: Event): Boolean {
        val eventWorld = when (event) {
            is LoadSlimeWorldEvent -> {
                if (unload) {
                    return false
                }
                event.slimeWorld.bukkitWorld
            }
            is SlimeWorldUnloadEvent -> {
                if (!unload) {
                    return false
                }
                event.world
            }
            else -> return false
        }

        val specifiedWorldNames = worldNames ?: return true
        return specifiedWorldNames.check(event) { worldName ->
            worldName.equals(eventWorld.name, ignoreCase = true)
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val action = if (unload) "unload" else "load"
        return "slime world $action" + (worldNames?.let { " of ${it.toString(event, debug)}" } ?: "")
    }
}
