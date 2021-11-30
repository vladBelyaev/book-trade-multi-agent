package ua.nure.bieliaiev.multiagent.wumpus

import aima.core.environment.wumpusworld.WumpusAction

enum class ActionEnum(val key: String, val action: WumpusAction) {
    FORWARD("forward", WumpusAction.FORWARD),
    TURN_LEFT("turn left", WumpusAction.TURN_LEFT),
    TURN_RIGHT("turn right", WumpusAction.TURN_RIGHT),
    GRAB("grab", WumpusAction.GRAB),
    SHOOT("shoot", WumpusAction.SHOOT),
    CLIMB("climb", WumpusAction.CLIMB);

    companion object {

        fun fromWumpusAction(wumpusAction: WumpusAction) = values().first { it.action == wumpusAction }
        fun fromKey(key: String): ActionEnum? = values().firstOrNull { it.key == key }
    }
}
