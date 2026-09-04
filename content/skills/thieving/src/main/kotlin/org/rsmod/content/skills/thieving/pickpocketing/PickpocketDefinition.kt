package org.rsmod.content.skills.thieving.pickpocketing

/**
 * One pickpocketable NPC category.
 *
 * [lowChance] / [highChance] feed directly into [org.rsmod.api.player.stat.statRandom]'s shared
 * formula, which computes `(1 + round(low + (high-low)*(level-1)/98)) / 256`. That's the wiki's
 * documented `low/256` and `high/256` endpoints **minus 1** each - see [PickpocketingData]'s class
 * doc for why.
 *
 * [stunTicks] is this NPC's failed-pickpocket "can't retry this NPC" lock. The generic Thieving
 * page states 8 ticks (4.8s) as the baseline; several NPC infoboxes give a rounded "5s"/"4s"/"6s"
 * figure instead of an exact tick count - those are approximated to the nearest tick pending
 * exact cache verification (see the class doc on [PickpocketingData]).
 */
internal data class PickpocketDefinition(
    val id: String,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val stunTicks: Int,
    val stunDamageMin: Int,
    val stunDamageMax: Int,
    /**
     * Bonus item roll on top of the guaranteed coin pouch, only set where a specific bonus-loot
     * table is wiki-verified (see [PickpocketingDrops]). Null means coin pouch only - correct for
     * NPCs where I don't yet have verified bonus-loot data, not a placeholder to fill in blindly.
     */
    val dropTableId: String? = null,
    val coinPouch: String? = null,
    val npcInternals: Set<String> = emptySet(),
    val category: Int? = null,
)
