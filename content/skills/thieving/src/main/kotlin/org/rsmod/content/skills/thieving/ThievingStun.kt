package org.rsmod.content.skills.thieving

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

/**
 * A failed thieving action (pickpocket, stall guard-aggro is handled separately, safe/chest trap)
 * has two independent consequences, per the OSRS wiki's Thieving page:
 * - The player cannot **move** for [MOVE_LOCK_TICKS] ticks (a fixed 9 ticks / 5.4s, the same for
 *   every NPC/activity).
 * - The player cannot **retry that specific action** for an activity-specific number of ticks
 *   (see [PickpocketDefinition.stunTicks][org.rsmod.content.skills.thieving.pickpocketing.PickpocketDefinition]).
 *
 * The move-lock reuses the engine's generic `frozen` flag (the same primitive
 * [org.rsmod.api.combat.commons.CombatEffects.freeze] uses for movement-lock during combat), but
 * with its own timer and message, and deliberately does **not** touch `freezeImmune` - a thieving
 * stun and a magic freeze spell are unrelated mechanics in real OSRS and shouldn't share immunity.
 */
public object ThievingStun {
    public const val MOVE_LOCK_TICKS: Int = 9

    private val RETRY_LOCK_UNTIL: AttributeKey<Int> = AttributeKey()

    public fun apply(target: Player, retryLockTicks: Int) {
        if (!target.isFrozen) {
            target.frozen = true
            target.routeDestination.clear()
            target.timer("timer.thieving_stun", MOVE_LOCK_TICKS)
            target.mes("You have been stunned!", ChatType.Spam)
            target.spotanim("spotanim.stunned_thieving", height = 100)
        }
        target.attr[RETRY_LOCK_UNTIL] = target.currentMapClock + retryLockTicks
    }

    /**
     * A *successful* pickpocket has no move-lock, but per the wiki's own averaged-ticks formula
     * ("every pickpocket will take on average 2 + 8(1-p) ticks") there's still a flat 2-tick
     * minimum before you can act again - this applies just that half of [apply], without freezing
     * movement.
     */
    public fun applyRetryLockOnly(target: Player, retryLockTicks: Int) {
        target.attr[RETRY_LOCK_UNTIL] = target.currentMapClock + retryLockTicks
    }

    public fun clearMoveLock(target: Player) {
        target.frozen = false
        target.clearTimer("timer.thieving_stun")
    }

    public fun isRetryLocked(target: Player): Boolean {
        val until = target.attr[RETRY_LOCK_UNTIL] ?: return false
        return target.currentMapClock < until
    }

    /** Clears both locks immediately - used by audit tooling to run attempts back-to-back. */
    public fun clearAll(target: Player) {
        clearMoveLock(target)
        target.attr.remove(RETRY_LOCK_UNTIL)
    }
}
