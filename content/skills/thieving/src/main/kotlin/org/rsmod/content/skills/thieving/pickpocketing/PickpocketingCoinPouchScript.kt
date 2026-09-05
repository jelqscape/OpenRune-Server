package org.rsmod.content.skills.thieving.pickpocketing

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * "Up to 28 coin pouches can stack in a player's inventory, after which the player must open them
 * to collect the coins inside before being able to pickpocket more." - the wiki's `Coin pouch`
 * page. [CoinPouches.CAP] is that base 28; the Ardougne Diary increases it further
 * (56/84/140 at Medium/Hard/Elite) but that bonus isn't wired up yet.
 *
 * Every coin pouch obj shares cache category 1249 with `Open-all` at op1 and `Open` at op2
 * (verified directly against several pouches' cache entries), so pouches are discovered
 * dynamically rather than hand-listed - a new coin pouch added to the cache is picked up
 * automatically as long as its min/max coin amount is added to [COIN_AMOUNTS].
 */
class PickpocketingCoinPouchScript
@Inject
constructor(
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (item in ServerCacheManager.getItemTypes()) {
            if (item.category != CoinPouches.CATEGORY) continue
            val amount = COIN_AMOUNTS[item.internalName] ?: continue
            onOpHeld1(item.internalName) { openPouch(it.slot, item.internalName, amount, all = true) }
            onOpHeld2(item.internalName) { openPouch(it.slot, item.internalName, amount, all = false) }
        }
    }

    private fun ProtectedAccess.openPouch(slot: Int, item: String, amount: IntRange, all: Boolean) {
        val invObj = inv[slot]?.takeIf { it.isType(item) } ?: return
        val count = if (all) invObj.count else 1

        if (count <= 0 || invDel(inv, item, count, slot = slot).failure) {
            return
        }

        val coins = (1..count).sumOf { random.of(amount) }
        invAdd(inv, "obj.coins", coins)
        mes("You open the coin pouch and find $coins coins.")
    }

    private companion object {
        /**
         * Min/max coins per pouch source, cross-checked against the wiki's own `Coin pouch` page
         * (Man x3, Guard x30, Knight of Ardougne x50, and every other exact/ranged amount below
         * all sourced from there directly, not estimated).
         */
        val COIN_AMOUNTS: Map<String, IntRange> =
            mapOf(
                "obj.pickpocket_coin_pouch_citizen" to 3..3,
                "obj.pickpocket_coin_pouch_farmer" to 9..9,
                "obj.pickpocket_coin_pouch_ham" to 1..21,
                "obj.pickpocket_coin_pouch_warrior" to 18..18,
                "obj.pickpocket_coin_pouch_rogue" to 25..40,
                "obj.pickpocket_coin_pouch_cavegoblin" to 10..50,
                "obj.pickpocket_coin_pouch_guard" to 30..30,
                "obj.pickpocket_coin_pouch_fremennik" to 40..40,
                "obj.pickpocket_coin_pouch_desertbandit" to 30..30,
                "obj.pickpocket_coin_pouch_knight" to 50..50,
                "obj.pickpocket_coin_pouch_watchman" to 60..60,
                "obj.pickpocket_coin_pouch_paladin" to 80..80,
                "obj.pickpocket_coin_pouch_gnome" to 300..300,
                "obj.pickpocket_coin_pouch_hero" to 200..300,
                "obj.pickpocket_coin_pouch_varlamore_wealthy" to 85..85,
                "obj.pickpocket_coin_pouch_vyre" to 230..315,
                "obj.pickpocket_coin_pouch_elf" to 280..350,
            )
    }
}

/** Shared with [org.rsmod.content.skills.thieving.pickpocketing.PickpocketingEvents]'s cap check. */
internal object CoinPouches {
    /** Cache category shared by every `pickpocket_coin_pouch_*` obj. */
    const val CATEGORY = 1249

    /** Base cap before the (not yet implemented) Ardougne Diary bonus. */
    const val CAP = 28

    fun heldCount(inv: Inventory): Int =
        inv.sumOf { obj ->
            if (obj != null && ServerCacheManager.getItem(obj.id)?.category == CATEGORY) obj.count else 0
        }
}
