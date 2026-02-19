package dev.danielmillar.slimelink.skript

import ch.njol.skript.expressions.base.PropertyExpression
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptEvent
import dev.danielmillar.slimelink.SlimeLink
import org.bukkit.event.Event as BukkitEvent
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.DefaultSyntaxInfos
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.registration.SyntaxRegistry

private fun syntaxRegistry(): SyntaxRegistry = SlimeLink.skriptAddon.syntaxRegistry()

fun <E : Condition> registerCondition(conditionClass: Class<E>, vararg patterns: String) {
    syntaxRegistry().register(
        SyntaxRegistry.CONDITION,
        SyntaxInfo.builder(conditionClass)
            .priority(SyntaxInfo.COMBINED)
            .addPatterns(*patterns)
            .build()
    )
}

fun <E : Effect> registerEffect(effectClass: Class<E>, vararg patterns: String) {
    syntaxRegistry().register(
        SyntaxRegistry.EFFECT,
        SyntaxInfo.builder(effectClass)
            .addPatterns(*patterns)
            .build()
    )
}

fun <E : SkriptEvent> registerEvent(
    eventClass: Class<E>,
    name: String,
    events: Array<Class<out BukkitEvent>>,
    vararg patterns: String,
    supplier: () -> E
) {
    val builder = BukkitSyntaxInfos.Event.builder(eventClass, name)
        .addPatterns(*patterns)
        .supplier(supplier)

    for (event in events) {
        builder.addEvent(event)
    }

    syntaxRegistry().register(BukkitSyntaxInfos.Event.KEY, builder.build())
}

fun <E : Expression<T>, T : Any> registerSimpleExpression(
    expressionClass: Class<E>,
    returnType: Class<T>,
    vararg patterns: String
) {
    registerExpression(expressionClass, returnType, SyntaxInfo.SIMPLE, *patterns)
}

fun <E : Expression<T>, T : Any> registerCombinedExpression(
    expressionClass: Class<E>,
    returnType: Class<T>,
    vararg patterns: String
) {
    registerExpression(expressionClass, returnType, SyntaxInfo.COMBINED, *patterns)
}

fun <E : Expression<T>, T : Any> registerPropertyExpression(
    expressionClass: Class<E>,
    returnType: Class<T>,
    vararg patterns: String
) {
    registerExpression(expressionClass, returnType, PropertyExpression.DEFAULT_PRIORITY, *patterns)
}

private fun <E : Expression<T>, T : Any> registerExpression(
    expressionClass: Class<E>,
    returnType: Class<T>,
    priority: org.skriptlang.skript.util.Priority,
    vararg patterns: String
) {
    syntaxRegistry().register(
        SyntaxRegistry.EXPRESSION,
        DefaultSyntaxInfos.Expression.builder(expressionClass, returnType)
            .priority(priority)
            .addPatterns(*patterns)
            .build()
    )
}
