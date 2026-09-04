package org.rsmod.content.skills.thieving.pickpocketing

import dev.openrune.types.NpcServerType

/**
 * Thieving level 1-90 pickpocketable NPCs. Every `level`/`xp`/`lowChance`/`highChance` value below
 * is sourced directly from that NPC's own OSRS wiki page (either stated in prose or back-solved
 * from the page's exact per-level success-chance chart), not estimated. `category` ids are
 * verified against this server's own NPC cache (`Man`=266, `Guard`=470, `Knight of Ardougne`=1731,
 * `TzHaar-Hur`=431, `Vyre`=1451, `Wealthy citizen`=1997 all spot-checked directly).
 *
 * **`lowChance`/`highChance` are the wiki's stated `low/256`/`high/256` endpoints minus 1, not the
 * raw wiki values.** [org.rsmod.api.utils.skills.SkillingSuccessRate.successRate] (the shared
 * formula every skill uses, via [org.rsmod.api.player.stat.statRandom]) computes
 * `(1 + round(low + (high-low)*(level-1)/98)) / 256` - i.e. it already adds 1 - so passing the raw
 * wiki numbers double-counts that +1 and overshoots by 1/256 at every level. Subtracting 1 up
 * front makes the formula's actual output match the wiki exactly; verified level-1 and level-99
 * for every entry below against the source chart before adopting this. (TzHaar-Hur's negative
 * `lowChance` is real, not a mistake - its curve is unusually steep and was independently
 * back-solved from its chart data; see the commit history for the derivation.)
 *
 * Stun tick counts: the Thieving overview page gives an exact baseline of 8 ticks (4.8s) for the
 * "can't retry this NPC" lock, and separately a fixed 9-tick move-lock shared by every NPC (see
 * [org.rsmod.content.skills.thieving.ThievingStun]). Several NPC infoboxes instead give a rounded
 * "5s"/"4s"/"6s" figure; where the wiki's own chart data doesn't resolve to a specific tick count,
 * these are approximated to the nearest tick (5s -> 8 ticks, 4s -> 7 ticks, 6s -> 10 ticks) per
 * that page's own caveat that these are rounded display values, not exact game data.
 *
 * Cave goblin and Fremennik citizen are deliberately **not** included: their wiki pages have no
 * success-chance chart or prose at all (re-checked twice), so their real formula endpoints aren't
 * verifiable from the wiki. Villager is excluded too - post-quest it can only be pickpocketed via
 * the (unimplemented) blackjack-knockout method, and grants 0 xp through a normal pickpocket
 * attempt, which this system doesn't have a code path for.
 *
 * TzHaar-Hur additionally burns the player for damage **on every successful pickpocket** unless
 * they're wearing ice gloves - that's a real, wiki-confirmed mechanic this implementation doesn't
 * apply yet (only the standard fail-stun damage below is wired up).
 */
internal object PickpocketingData {
    internal val definitions: List<PickpocketDefinition> =
        listOf(
            define(
                id = "citizen",
                level = 1,
                xp = 8.0,
                lowChance = 180,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 1,
                stunDamageMax = 1,
                coinPouch = "obj.pickpocket_coin_pouch_citizen",
                category = 266,
            ),
            define(
                id = "farmer",
                level = 10,
                xp = 14.5,
                lowChance = 150,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 1,
                stunDamageMax = 1,
                dropTableId = "farmer",
                coinPouch = "obj.pickpocket_coin_pouch_farmer",
                category = 498,
            ),
            define(
                id = "ham_member",
                level = 15,
                xp = 22.2,
                lowChance = 135,
                highChance = 239,
                stunTicks = 7,
                stunDamageMin = 1,
                stunDamageMax = 3,
                coinPouch = "obj.pickpocket_coin_pouch_ham",
                npcInternals = setOf("npc.favour_male_ham_civilian", "npc.favour_female_ham_civilian"),
            ),
            define(
                id = "warrior",
                level = 25,
                xp = 26.0,
                lowChance = 100,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 2,
                stunDamageMax = 2,
                coinPouch = "obj.pickpocket_coin_pouch_warrior",
                category = 1728,
            ),
            define(
                id = "rogue",
                level = 32,
                xp = 36.5,
                lowChance = 75,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 2,
                stunDamageMax = 2,
                dropTableId = "rogue",
                coinPouch = "obj.pickpocket_coin_pouch_rogue",
                npcInternals = setOf("npc.rogue", "npc.wilderness_rogue"),
            ),
            define(
                id = "master_farmer",
                level = 38,
                xp = 43.0,
                lowChance = 90,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 3,
                stunDamageMax = 3,
                dropTableId = "master_farmer",
                category = 641,
            ),
            define(
                id = "guard",
                level = 40,
                xp = 46.8,
                lowChance = 50,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 2,
                stunDamageMax = 2,
                coinPouch = "obj.pickpocket_coin_pouch_guard",
                category = 470,
            ),
            define(
                id = "desert_bandit",
                level = 53,
                xp = 79.4,
                lowChance = 50,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 3,
                stunDamageMax = 3,
                dropTableId = "desert_bandit",
                coinPouch = "obj.pickpocket_coin_pouch_desertbandit",
                npcInternals =
                    setOf(
                        "npc.fourdiamonds_sword_bandit_1",
                        "npc.fourdiamonds_sword_bandit_free",
                    ),
            ),
            define(
                id = "knight_of_ardougne",
                level = 55,
                xp = 84.3,
                lowChance = 50,
                highChance = 240,
                stunTicks = 8,
                stunDamageMin = 3,
                stunDamageMax = 3,
                coinPouch = "obj.pickpocket_coin_pouch_knight",
                category = 1731,
            ),
            define(
                id = "watchman",
                level = 65,
                xp = 137.5,
                lowChance = 15,
                highChance = 160,
                stunTicks = 8,
                stunDamageMin = 3,
                stunDamageMax = 3,
                dropTableId = "watchman",
                coinPouch = "obj.pickpocket_coin_pouch_watchman",
                npcInternals = setOf("npc.yanille_watchman"),
            ),
            define(
                id = "paladin",
                level = 70,
                xp = 131.8,
                lowChance = 40,
                highChance = 170,
                stunTicks = 8,
                stunDamageMin = 3,
                stunDamageMax = 3,
                dropTableId = "paladin",
                coinPouch = "obj.pickpocket_coin_pouch_paladin",
                category = 1729,
            ),
            define(
                id = "gnome",
                level = 75,
                xp = 133.3,
                lowChance = 43,
                highChance = 175,
                stunTicks = 8,
                stunDamageMin = 1,
                stunDamageMax = 1,
                dropTableId = "gnome",
                coinPouch = "obj.pickpocket_coin_pouch_gnome",
                category = 354,
            ),
            define(
                id = "hero",
                level = 80,
                xp = 163.3,
                lowChance = 39,
                highChance = 160,
                stunTicks = 10,
                stunDamageMin = 3,
                stunDamageMax = 3,
                dropTableId = "hero",
                coinPouch = "obj.pickpocket_coin_pouch_hero",
                category = 1730,
            ),
            define(
                id = "wealthy_citizen",
                level = 50,
                xp = 96.0,
                lowChance = 35,
                highChance = 200,
                stunTicks = 7,
                stunDamageMin = 3,
                stunDamageMax = 3,
                dropTableId = "wealthy_citizen",
                coinPouch = "obj.pickpocket_coin_pouch_varlamore_wealthy",
                category = 1997,
            ),
            define(
                id = "vyre",
                level = 82,
                xp = 306.9,
                lowChance = 8,
                highChance = 128,
                stunTicks = 10,
                stunDamageMin = 5,
                stunDamageMax = 5,
                dropTableId = "vyre",
                coinPouch = "obj.pickpocket_coin_pouch_vyre",
                category = 1451,
            ),
            define(
                id = "elf",
                level = 85,
                xp = 353.3,
                lowChance = 6,
                highChance = 100,
                stunTicks = 10,
                stunDamageMin = 5,
                stunDamageMax = 5,
                dropTableId = "elf",
                coinPouch = "obj.pickpocket_coin_pouch_elf",
                npcInternals =
                    setOf(
                        "npc.mourning_town_elf_1",
                        "npc.mourning_town_elf_2",
                        "npc.mourning_town_elf_3",
                        "npc.mourning_town_elf_4",
                        "npc.mourning_town_elf_5",
                    ),
            ),
            define(
                id = "tzhaar_hur",
                level = 90,
                xp = 103.4,
                lowChance = -200,
                highChance = 200,
                stunTicks = 10,
                stunDamageMin = 4,
                stunDamageMax = 4,
                dropTableId = "tzhaar_hur",
                category = 431,
            ),
        )

    private val byNpcInternal: Map<String, PickpocketDefinition> =
        definitions.flatMap { row -> row.npcInternals.map { it to row } }.toMap()

    private val byCategory: Map<Int, PickpocketDefinition> =
        definitions.mapNotNull { row -> row.category?.let { it to row } }.toMap()

    fun resolve(type: NpcServerType): PickpocketDefinition? {
        byNpcInternal[type.internalName]?.let { return it }
        return byCategory[type.category]
    }
}

private fun define(
    id: String,
    level: Int,
    xp: Double,
    lowChance: Int,
    highChance: Int,
    stunTicks: Int,
    stunDamageMin: Int,
    stunDamageMax: Int,
    dropTableId: String? = null,
    coinPouch: String? = null,
    npcInternals: Set<String> = emptySet(),
    category: Int? = null,
): PickpocketDefinition =
    PickpocketDefinition(
        id = id,
        level = level,
        xp = xp,
        lowChance = lowChance,
        highChance = highChance,
        stunTicks = stunTicks,
        stunDamageMin = stunDamageMin,
        stunDamageMax = stunDamageMax,
        dropTableId = dropTableId,
        coinPouch = coinPouch,
        npcInternals = npcInternals,
        category = category,
    )
