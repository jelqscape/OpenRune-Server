package org.rsmod.content.skills.thieving.pickpocketing

import dtx.core.Rollable
import dtx.rs.RSGuaranteedTable
import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

/**
 * Reward tables for [PickpocketDefinition.dropTableId]. Every table here *is* the entire reward
 * for that NPC - a coin pouch, where one exists, is one weighted/guaranteed row among others, not
 * a bonus stacked on top of an automatic coin grant (see [PickpocketingEvents.giveRewards]).
 *
 * Clue scrolls are deliberately omitted from every table below (Paladin/Gnome/Hero/Wealthy citizen
 * all document one) - this cache has no single generic "clue scroll (tier)" item beyond
 * `obj.trail_clue_beginner`; every other tier is dozens of individually-named variants with no
 * random-variant-picker built yet. Their probability share is folded into "nothing" for now.
 */
internal object PickpocketingDrops {
    /** Farmer: coin pouch @ 123/128, Potato seed @ 5/128 (Drop Rate Project, 2,324,719 samples). */
    private val farmer: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 128) {
            name("Pickpocket - Farmer")
            123 weight "obj.pickpocket_coin_pouch_farmer" count 1
            5 weight "obj.potato_seed" count 1
        }

    /** Rogue: coin pouch @ 123/144, air runes/jug of wine/lockpick/iron dagger(p) (62,171 samples). */
    private val rogue: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 144) {
            name("Pickpocket - Rogue")
            123 weight "obj.pickpocket_coin_pouch_rogue" count 1
            9 weight "obj.airrune" count 8
            6 weight "obj.jug_wine" count 1
            5 weight "obj.lockpick" count 1
            1 weight "obj.iron_dagger_p" count 1
        }

    /**
     * Master Farmer: 485/1000 chance of an allotment seed (weights below are the wiki's own "1 in
     * X" endpoints converted to /1000, rounded, summing to 485 exactly), otherwise nothing - Master
     * Farmer has no coin pouch of its own. Only the allotment sub-table is included; the wiki also
     * documents separate hop/flower/bush/special/herb tables not verified here yet.
     */
    private val masterFarmer: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 1000) {
            name("Pickpocket - Master Farmer")
            515 weight nothing()
            177 weight "obj.potato_seed" count 1..4
            133 weight "obj.onion_seed" count 1..3
            69 weight "obj.cabbage_seed" count 1..3
            64 weight "obj.tomato_seed" count 1..2
            22 weight "obj.sweetcorn_seed" count 1..2
            11 weight "obj.strawberry_seed" count 1
            5 weight "obj.watermelon_seed" count 1
            4 weight "obj.snape_grass_seed" count 1
        }

    /** Desert bandit: coin pouch @ 5/7, Antipoison(1)/Lockpick @ 1/7 each (12,910 samples). */
    private val desertBandit: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 7) {
            name("Pickpocket - Desert bandit")
            5 weight "obj.pickpocket_coin_pouch_desertbandit" count 1
            1 weight "obj.1doseantipoison" count 1
            1 weight "obj.lockpick" count 1
        }

    /** Watchman: coin pouch AND bread, both guaranteed on every success (no roll involved). */
    private val watchman: RSGuaranteedTable<Player, DropRollItem> =
        rsPlayerGuaranteedTable {
            "obj.pickpocket_coin_pouch_watchman" count 1
            "obj.bread" count 1
        }

    /** Paladin: coin pouch AND 2 chaos runes, both guaranteed on every success. */
    private val paladin: RSGuaranteedTable<Player, DropRollItem> =
        rsPlayerGuaranteedTable {
            "obj.pickpocket_coin_pouch_paladin" count 1
            "obj.chaosrune" count 2
        }

    /** Gnome: real weighted table, no separate coin-pouch row - "Coins 300" @ 30/128 is the pouch
     * itself (DRP 11,857,551 samples, weights sum to 128). */
    private val gnome: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 128) {
            name("Pickpocket - Gnome")
            56 weight "obj.arrow_shaft" count 2..4
            30 weight "obj.pickpocket_coin_pouch_gnome" count 1
            24 weight "obj.swamp_toad" count 1
            8 weight "obj.gold_ore" count 1
            5 weight "obj.earthrune" count 1
            3 weight "obj.king_worm" count 1
            2 weight "obj.fire_orb" count 1
        }

    /** Hero: weighted table (no separate coin-pouch row, "coin pouch" is the 105/128 slot). Rogue
     * equipment's doubled-reward variant isn't implemented, so only the base amount is used. */
    private val hero: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 128) {
            name("Pickpocket - Hero")
            105 weight "obj.pickpocket_coin_pouch_hero" count 1
            8 weight "obj.deathrune" count 2
            6 weight "obj.jug_wine" count 1
            5 weight "obj.bloodrune" count 1
            2 weight "obj.fire_orb" count 1
            1 weight "obj.diamond" count 1
            1 weight "obj.gold_ore" count 1
        }

    /**
     * Wealthy citizen: coin pouch ~79/85, house keys ~5/85 (Varlamore thieving house key, not the
     * unrelated Feud mayor's key), rest folded into nothing (clue slot omitted). DRP 147,715
     * samples.
     */
    private val wealthyCitizen: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 85) {
            name("Pickpocket - Wealthy citizen")
            79 weight "obj.pickpocket_coin_pouch_varlamore_wealthy" count 1
            5 weight "obj.varlamore_thieving_house_key" count 1
            1 weight nothing()
        }

    /** Vyre: weighted table, coin pouch is the 109/132 slot (DRP 4,080,000 samples, weights sum to
     * 132). Blood shard (1/5,000, a separate roll per the wiki) isn't included. */
    private val vyre: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 132) {
            name("Pickpocket - Vyre")
            109 weight "obj.pickpocket_coin_pouch_vyre" count 1
            8 weight "obj.deathrune" count 2
            6 weight "obj.blood_pint" count 1
            5 weight "obj.uncut_ruby" count 1
            2 weight "obj.bloodrune" count 4
            1 weight "obj.diamond" count 1
            1 weight "obj.cooked_mystery_meat" count 1
        }

    /** Elf: weighted table (Lletya-era, 7-row version per the wiki), coin pouch is the 105/128
     * slot. DRP 1,234,317 samples. Prifddinas-only pre-roll items (crystal shard, enhanced crystal
     * teleport seed) aren't included. */
    private val elf: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 128) {
            name("Pickpocket - Elf")
            105 weight "obj.pickpocket_coin_pouch_elf" count 1
            8 weight "obj.deathrune" count 2
            6 weight "obj.jug_wine" count 1
            5 weight "obj.naturerune" count 3
            2 weight "obj.fire_orb" count 1
            1 weight "obj.diamond" count 1
            1 weight "obj.gold_ore" count 1
        }

    /**
     * TzHaar-Hur: no coin pouch at all - drops Tokkul directly. Rates from Mod Ash's own code
     * quote (Twitter, 12 Feb 2020), weights sum to 195.
     */
    private val tzhaarHur: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 195) {
            name("Pickpocket - TzHaar-Hur")
            182 weight "obj.tzhaar_token" count 3..7
            5 weight "obj.uncut_sapphire" count 1
            4 weight "obj.uncut_emerald" count 1
            3 weight "obj.uncut_ruby" count 1
            1 weight "obj.uncut_diamond" count 1
        }

    private val weightedTables: Map<String, RSWeightedTable<Player, DropRollItem>> =
        mapOf(
            "farmer" to farmer,
            "rogue" to rogue,
            "master_farmer" to masterFarmer,
            "desert_bandit" to desertBandit,
            "gnome" to gnome,
            "hero" to hero,
            "wealthy_citizen" to wealthyCitizen,
            "vyre" to vyre,
            "elf" to elf,
            "tzhaar_hur" to tzhaarHur,
        )

    private val guaranteedTables: Map<String, RSGuaranteedTable<Player, DropRollItem>> =
        mapOf(
            "watchman" to watchman,
            "paladin" to paladin,
        )

    /** Returns whichever table type this id maps to - callers only need `.roll(player, ArgMap())`
     * on the common `Rollable` interface both table types implement. */
    fun table(id: String?): Rollable<Player, DropRollItem>? {
        if (id == null) return null
        return weightedTables[id] ?: guaranteedTables[id]
    }
}
