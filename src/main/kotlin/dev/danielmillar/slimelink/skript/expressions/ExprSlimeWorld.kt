package dev.danielmillar.slimelink.skript.expressions

import ch.njol.skript.Skript
import ch.njol.skript.lang.EventRestrictedSyntax
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import dev.danielmillar.slimelink.events.SlimeWorldLoadEvent
import dev.danielmillar.slimelink.events.SlimeWorldUnloadEvent
import org.bukkit.World
import org.bukkit.event.Event

class ExprSlimeWorld : SimpleExpression<World>(), EventRestrictedSyntax {

    companion object {
        init {
            Skript.registerExpression(
                ExprSlimeWorld::class.java,
                World::class.java,
                ExpressionType.SIMPLE,
                "[the] (slimeworld|slime world)"
            )
        }

        private fun getSlimeWorld(event: Event): World? {
            return when (event) {
                is SlimeWorldLoadEvent -> event.slimeworld
                is SlimeWorldUnloadEvent -> event.slimeworld
                else -> null
            }
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        return true
    }

    override fun get(event: Event): Array<World?> {
        return arrayOf(getSlimeWorld(event))
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "slimeworld"
    }

    override fun isSingle(): Boolean {
        return true
    }

    override fun getReturnType(): Class<out World?> {
        return World::class.java
    }

    override fun supportedEvents(): Array<out Class<out Event?>?> {
        return arrayOf(SlimeWorldLoadEvent::class.java, SlimeWorldUnloadEvent::class.java)
    }
}