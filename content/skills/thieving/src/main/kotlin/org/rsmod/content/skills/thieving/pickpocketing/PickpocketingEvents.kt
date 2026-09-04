package org.rsmod.content.skills.thieving.pickpocketing

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcServerType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.content.skills.thieving.ThievingStun
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Pickpocketing dynamically registers against every NPC in the cache that already carries a
 * "Pickpocket" op (this revision's cache has it pre-wired at op-index 2 / `onOpNpc3` for every
 * such NPC) and that [PickpocketingData] resolves a definition for - no hand-maintained per-map
 * NPC list needed.
 */
class PickpocketingEvents
@Inject
constructor(
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npcType in ServerCacheManager.getNpcs().values) {
            val slot = pickpocketOpSlot(npcType) ?: continue
            val definition = PickpocketingData.resolve(npcType) ?: continue
            registerPickpocketOp(npcType.internalName, slot, definition)
        }
    }

    private fun ScriptContext.registerPickpocketOp(
        internal: String,
        slot: Int,
        definition: PickpocketDefinition,
    ) {
        when (slot) {
            1 -> onOpNpc1(internal) { attemptPickpocket(it.npc, definition) }
            2 -> onOpNpc2(internal) { attemptPickpocket(it.npc, definition) }
            3 -> onOpNpc3(internal) { attemptPickpocket(it.npc, definition) }
            4 -> onOpNpc4(internal) { attemptPickpocket(it.npc, definition) }
            5 -> onOpNpc5(internal) { attemptPickpocket(it.npc, definition) }
        }
    }

    private fun pickpocketOpSlot(type: NpcServerType): Int? {
        for (slot in 1..5) {
            val action = type.actions.getOpOrNull(slot - 1)?.trim()?.lowercase()
            if (action == "pickpocket") {
                return slot
            }
        }
        return null
    }

    private suspend fun ProtectedAccess.attemptPickpocket(npc: Npc, definition: PickpocketDefinition) {
        performPickpocketAttempt(npc, definition)
    }

    /**
     * The real, shared attempt logic - the click-driven op handler above and
     * [PickpocketingAuditScript] both call this exact function, so auditing a definition exercises
     * the same success roll, reward path, and stun consequence a real player would hit. Returns
     * whether the attempt succeeded (used by the op handler to decide whether to auto-repeat).
     */
    internal fun ProtectedAccess.performPickpocketAttempt(
        npc: Npc,
        definition: PickpocketDefinition,
    ): Boolean {
        val blockMessage = cannotPickpocket(definition)
        if (blockMessage != null) {
            mes(blockMessage)
            return false
        }

        val npcName = npc.visType.name
        val success = statRandom("stat.thieving", definition.lowChance, definition.highChance, 0)

        anim("seq.human_pickpocket")

        if (success) {
            mes("You successfully pick the $npcName's pocket.")
            giveRewards(npc, definition)
            if (definition.id == "tzhaar_hur" && "obj.ice_gloves" !in player.worn) {
                // Wiki: "If ice gloves are not worn when pickpocketing the player will take 4
                // damage, regardless of success; if ice gloves are worn, the player will only
                // take 4 damage upon failure." Failure damage below is unconditional either way.
                queueHit(source = npc, delay = 1, type = HitType.Typeless, damage = TZHAAR_BURN_DAMAGE)
            }
            // No move-lock on success, but the wiki's own averaged-ticks formula ("every
            // pickpocket will take on average 2 + 8(1-p) ticks") confirms a flat 2-tick minimum
            // before you can act again even when nothing goes wrong.
            ThievingStun.applyRetryLockOnly(player, SUCCESS_DELAY_TICKS)
            return true
        }

        mes("You fail to pick the $npcName's pocket.")
        npc.facePlayer(player)
        val damage = random.of(definition.stunDamageMin..definition.stunDamageMax)
        queueHit(source = npc, delay = 1, type = HitType.Typeless, damage = damage)
        ThievingStun.apply(player, definition.stunTicks)
        return false
    }

    private fun ProtectedAccess.cannotPickpocket(definition: PickpocketDefinition): String? {
        if (ThievingStun.isRetryLocked(player)) {
            return "You're still recovering from your last attempt."
        }
        if (player.thievingLvl < definition.level) {
            return "You need a Thieving level of ${definition.level} to pickpocket this NPC."
        }
        if (inv.isFull()) {
            return "You don't have enough inventory space to do this."
        }
        return null
    }

    /**
     * XP is always granted on a successful pickpocket, independent of loot. Loot itself splits
     * into two shapes, matching how the wiki's own per-NPC tables are laid out:
     * - **No [PickpocketDefinition.dropTableId]**: [PickpocketDefinition.coinPouch] is the only
     *   possible reward and is always given (e.g. Man, Guard, Knight of Ardougne - "Coins, Always"
     *   with nothing else on the table).
     * - **With a `dropTableId`**: the table is the *entire* reward, and a coin pouch (if this NPC
     *   has one) is one of the table's own weighted/guaranteed rows, not a separate unconditional
     *   grant - e.g. Rogue's wiki table is "coins 123/144, air runes 9/144, ..." (mutually
     *   exclusive outcomes, not "always coins, occasionally *also* a rune").
     */
    private fun ProtectedAccess.giveRewards(npc: Npc, definition: PickpocketDefinition) {
        statAdvance("stat.thieving", definition.xp)

        val table = PickpocketingDrops.table(definition.dropTableId)
        if (table == null) {
            definition.coinPouch?.let { invAdd(inv, it, 1) }
        } else {
            when (val result = table.roll(player, ArgMap()).flatten()) {
                is RollResult.Nothing -> Unit
                is RollResult.Single -> giveDrop(result.result)
                is RollResult.ListOf -> result.results.forEach { giveDrop(it) }
            }
        }

        with(RockyPet) { rollRocky() }
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        val obj = drop.transformObj(player) ?: drop.obj
        val amount = drop.rollCount(random)
        if (amount <= 0) return
        invAdd(inv, obj, amount)
    }

    private companion object {
        const val TZHAAR_BURN_DAMAGE = 4
        const val SUCCESS_DELAY_TICKS = 2
    }
}
