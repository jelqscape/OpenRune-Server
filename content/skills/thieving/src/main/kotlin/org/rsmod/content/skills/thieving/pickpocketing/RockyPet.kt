package org.rsmod.content.skills.thieving.pickpocketing

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase

/**
 * Rocky, the Thieving skilling pet. Rolled on every successful pickpocket (Rocky rolls on other
 * thieving actions too in real OSRS - not implemented since stalls/chests/doors don't exist yet).
 *
 * Rate: `1/(176743 - thievingLevel*25)`, per Mod Roq - cited identically on every pickpocketable
 * NPC's own wiki page (e.g. Man: 1/176743 at level 1 down to 1/152218 at level 99).
 */
internal object RockyPet {
    private const val PET_OBJ: String = "obj.skillpetthieving"
    private const val BASE_CHANCE: Int = 176_743
    private const val LEVEL_SCALING: Int = 25

    fun ProtectedAccess.rollRocky() {
        val chance = (BASE_CHANCE - player.statBase("stat.thieving") * LEVEL_SCALING).coerceAtLeast(1)
        if (random.of(chance) != 0 || inv.isFull()) {
            return
        }
        invAdd(inv, PET_OBJ, 1)
        // TODO: spawn Rocky as a follower once a pet system exists; until then it lands in inv.
        spam("You have a funny feeling like you're being followed.")
    }
}
