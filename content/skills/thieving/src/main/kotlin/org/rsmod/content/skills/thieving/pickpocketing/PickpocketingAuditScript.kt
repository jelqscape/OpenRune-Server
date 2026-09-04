package org.rsmod.content.skills.thieving.pickpocketing

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCM.getReverseMapping
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onCommand
import org.rsmod.content.skills.thieving.ThievingStun
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Debug-only: sweeps every known [PickpocketDefinition] one at a time - spawns it, pickpockets it
 * [attemptsPerNpc] times through the exact same [PickpocketingEvents.performPickpocketAttempt]
 * real players hit, despawns it, then moves on to the next - so the whole roster can be smoke-
 * tested in a single `::thieveaudit` with nothing else to type. No hand-rolled duplicate of the
 * roll or reward logic, so a run genuinely reflects live behavior.
 *
 * Usage: `::thieveaudit [attemptsPerNpc]` (default 1). `::thieveaudit <id> [attemptsPerNpc]` runs
 * just one NPC, for digging into a specific result.
 */
class PickpocketingAuditScript
@Inject
constructor(
    private val events: PickpocketingEvents,
    private val npcRepo: NpcRepository,
    private val launcher: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("thieveaudit") {
            desc = "Sweep-test every pickpocketable NPC"
            requiredRights = Rights.ADMINISTRATOR
            invalidArgs =
                "Use as ::thieveaudit [attemptsPerNpc] or ::thieveaudit id [attemptsPerNpc] - ids: " +
                    PickpocketingData.definitions.joinToString(", ") { it.id }
            cheat(::audit)
        }
    }

    private fun audit(cheat: Cheat) =
        with(cheat) {
            val explicitId = args.getOrNull(0)?.takeIf { PickpocketingData.definitions.any { d -> d.id == it } }
            val definitions =
                if (explicitId != null) {
                    listOf(PickpocketingData.definitions.first { it.id == explicitId })
                } else {
                    PickpocketingData.definitions
                }

            val attemptsArgIndex = if (explicitId != null) 1 else 0
            val attemptsPerNpc = args.getOrNull(attemptsArgIndex)?.toIntOrNull()?.coerceIn(1, 200) ?: 1

            if (args.isNotEmpty() && explicitId == null && args[0].toIntOrNull() == null) {
                player.mes(
                    "Unknown id '${args[0]}'. Known: " +
                        PickpocketingData.definitions.joinToString(", ") { it.id },
                )
                return@with
            }

            val started =
                launcher.launch(player, busyText = "You're busy right now.") {
                    sweep(definitions, attemptsPerNpc)
                }
            if (!started) {
                player.mes("Couldn't start the audit - you're mid-action.")
            }
        }

    private suspend fun ProtectedAccess.sweep(definitions: List<PickpocketDefinition>, attemptsPerNpc: Int) {
        statAdvance("stat.thieving", PlayerSkillXPTable.getXPFromLevel(99).toDouble())

        var totalSuccesses = 0
        var totalAttempts = 0
        var totalXp = 0.0
        val grandItemTotals = mutableMapOf<String, Int>()

        for (definition in definitions) {
            val npcType = resolveSpawnableNpc(definition)
            if (npcType == null) {
                mes("${definition.id}: no spawnable npc found (check npcInternals/category).")
                continue
            }

            val npc = Npc(npcType, player.coords.translate(Direction.North))
            npc.mode = NpcMode.None
            npcRepo.add(npc, duration = Int.MAX_VALUE)
            // Without this, the whole add -> attempts -> del cycle can complete inside a single
            // tick, and the NPC never survives long enough for the server's per-tick NPC-info
            // broadcast to actually tell the client it exists - it "works" (attempts still roll
            // correctly) but is never visible.
            delay(1)

            var successes = 0
            var xpGained = 0.0
            val itemTotals = mutableMapOf<String, Int>()

            repeat(attemptsPerNpc) {
                ThievingStun.clearAll(player)

                val before = snapshotInv()
                val success = with(events) { performPickpocketAttempt(npc, definition) }
                if (success) {
                    successes++
                    xpGained += definition.xp
                    tallyGained(before, snapshotInv(), itemTotals)
                }

                if (inv.isFull()) {
                    player.invClear(inv)
                }

                // A failed attempt queues a hit landing 1 tick later - wait it out before the next
                // attempt (or the next NPC's), otherwise several NPCs' fail-damage hits can land on
                // the same tick and pile up into a confusing stack of hitsplats.
                delay(2)
            }

            ThievingStun.clearAll(player)
            npcRepo.del(npc, duration = Int.MAX_VALUE)

            val itemSummary =
                if (itemTotals.isEmpty()) "" else " - " + itemTotals.entries.joinToString(", ") { "${it.key} x${it.value}" }
            mes(
                "${definition.id}: $successes/$attemptsPerNpc success, " +
                    "${"%.1f".format(xpGained)} xp$itemSummary",
            )

            totalSuccesses += successes
            totalAttempts += attemptsPerNpc
            totalXp += xpGained
            for ((item, count) in itemTotals) {
                grandItemTotals[item] = (grandItemTotals[item] ?: 0) + count
            }
        }

        val rate = if (totalAttempts > 0) totalSuccesses * 100.0 / totalAttempts else 0.0
        mes(
            "Thieving audit done: $totalSuccesses/$totalAttempts success (${"%.1f".format(rate)}%), " +
                "${"%.1f".format(totalXp)} xp total across ${definitions.size} npcs.",
        )
    }

    private fun ProtectedAccess.snapshotInv(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (obj in inv) {
            if (obj == null) continue
            val name = getReverseMapping(RSCMType.OBJ, obj.id)
            counts[name] = (counts[name] ?: 0) + obj.count
        }
        return counts
    }

    private fun tallyGained(
        before: Map<String, Int>,
        after: Map<String, Int>,
        totals: MutableMap<String, Int>,
    ) {
        for ((item, afterCount) in after) {
            val delta = afterCount - (before[item] ?: 0)
            if (delta > 0) {
                totals[item] = (totals[item] ?: 0) + delta
            }
        }
    }

    private fun resolveSpawnableNpc(definition: PickpocketDefinition): NpcServerType? {
        definition.npcInternals.firstOrNull()?.let { internal ->
            ServerCacheManager.getNpc(internal.asRSCM(RSCMType.NPC))?.let { return it }
        }
        val category = definition.category ?: return null
        return ServerCacheManager.getNpcs().values.firstOrNull { it.category == category }
    }
}
