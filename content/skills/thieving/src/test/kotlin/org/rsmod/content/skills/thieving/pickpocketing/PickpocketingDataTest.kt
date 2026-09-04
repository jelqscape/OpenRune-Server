package org.rsmod.content.skills.thieving.pickpocketing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.utils.skills.SkillingSuccessRate

/**
 * Regression-pins the wiki-sourced level/xp/lowChance/highChance/stun values in
 * [PickpocketingData] so a future edit can't silently drift them, and cross-checks the
 * success-chance endpoints against the actual shared [SkillingSuccessRate] formula every skill
 * uses, confirming it reproduces the real wiki chart exactly (not off by the formula's own +1 -
 * see [PickpocketingData]'s class doc for why `lowChance`/`highChance` are the wiki values minus
 * 1, not the raw wiki numbers).
 */
class PickpocketingDataTest {
    private fun definition(id: String): PickpocketDefinition =
        PickpocketingData.definitions.single { it.id == id }

    @Test
    fun `citizen (Man Woman) matches the wiki-documented Man endpoints`() {
        val row = definition("citizen")
        assertEquals(1, row.level)
        assertEquals(8.0, row.xp)
        assertEquals(180, row.lowChance)
        assertEquals(240, row.highChance)
        assertEquals(266, row.category)
    }

    @Test
    fun `guard matches the wiki-verified endpoints, not the wrong third-party 180-241 value`() {
        val row = definition("guard")
        assertEquals(40, row.level)
        assertEquals(50, row.lowChance)
        assertEquals(240, row.highChance)
        assertEquals(470, row.category)
    }

    @Test
    fun `knight of ardougne matches the wiki-verified endpoints, not the wrong third-party 180-241 value`() {
        val row = definition("knight_of_ardougne")
        assertEquals(55, row.level)
        assertEquals(50, row.lowChance)
        assertEquals(240, row.highChance)
        assertEquals(1731, row.category)
    }

    @Test
    fun `master farmer matches the wiki-quoted 91-241 endpoints`() {
        val row = definition("master_farmer")
        assertEquals(38, row.level)
        assertEquals(90, row.lowChance)
        assertEquals(240, row.highChance)
        assertEquals(641, row.category)
    }

    @Test
    fun `tzhaar-hur's negative lowChance is intentional, not a typo`() {
        val row = definition("tzhaar_hur")
        assertEquals(90, row.level)
        assertEquals(-200, row.lowChance)
        assertEquals(200, row.highChance)
        assertEquals(431, row.category)
    }

    @Test
    fun `vyre and elf match their back-solved chart endpoints`() {
        val vyre = definition("vyre")
        assertEquals(8, vyre.lowChance)
        assertEquals(128, vyre.highChance)
        assertEquals(1451, vyre.category)

        val elf = definition("elf")
        assertEquals(6, elf.lowChance)
        assertEquals(100, elf.highChance)
        assertEquals(5, elf.npcInternals.size)
    }

    @Test
    fun `every definition has a level between 1 and 99`() {
        for (row in PickpocketingData.definitions) {
            assertTrue(row.level in 1..99, "${row.id} has an out-of-range level: ${row.level}")
        }
    }

    @Test
    fun `every definition has highChance within 0-256 and low never exceeds high`() {
        // lowChance may legitimately be negative (see tzhaar_hur) - only highChance is bounded.
        for (row in PickpocketingData.definitions) {
            assertTrue(row.highChance in 0..256, "${row.id} highChance out of range: ${row.highChance}")
            assertTrue(
                row.lowChance <= row.highChance,
                "${row.id} has lowChance (${row.lowChance}) > highChance (${row.highChance})",
            )
        }
    }

    @Test
    fun `every definition resolves via at least one npc internal name or category`() {
        for (row in PickpocketingData.definitions) {
            assertTrue(
                row.npcInternals.isNotEmpty() || row.category != null,
                "${row.id} has no npcInternals and no category - it can never be resolved",
            )
        }
    }

    @Test
    fun `no two definitions share the same category id`() {
        val categories = PickpocketingData.definitions.mapNotNull { it.category }
        assertEquals(categories.size, categories.toSet().size, "Duplicate category id across definitions")
    }

    @Test
    fun `no two definitions share the same npc internal name`() {
        val internals = PickpocketingData.definitions.flatMap { it.npcInternals }
        assertEquals(internals.size, internals.toSet().size, "Duplicate npc internal name across definitions")
    }

    @Test
    fun `stat success formula reproduces the Man wiki chart exactly`() {
        // Wiki chart: 181/256 at level 1, 241/256 at level 99.
        val row = definition("citizen")
        val atLevel1 = SkillingSuccessRate.successRate(row.lowChance, row.highChance, level = 1, maxLevel = 99)
        val atLevel99 = SkillingSuccessRate.successRate(row.lowChance, row.highChance, level = 99, maxLevel = 99)
        assertEquals(181.0 / 256.0, atLevel1, 1e-9)
        assertEquals(241.0 / 256.0, atLevel99, 1e-9)
    }

    @Test
    fun `stat success formula reproduces the Guard wiki chart exactly`() {
        // Wiki chart: 127/256 at level 40 (the requirement level).
        val row = definition("guard")
        val atLevel40 = SkillingSuccessRate.successRate(row.lowChance, row.highChance, level = 40, maxLevel = 99)
        assertEquals(127.0 / 256.0, atLevel40, 1e-9)
    }

    @Test
    fun `stat success formula reproduces the Vyre wiki chart exactly`() {
        // Wiki chart: 108/256 at level 82, 129/256 at level 99.
        val row = definition("vyre")
        val atLevel82 = SkillingSuccessRate.successRate(row.lowChance, row.highChance, level = 82, maxLevel = 99)
        val atLevel99 = SkillingSuccessRate.successRate(row.lowChance, row.highChance, level = 99, maxLevel = 99)
        assertEquals(108.0 / 256.0, atLevel82, 1e-9)
        assertEquals(129.0 / 256.0, atLevel99, 1e-9)
    }
}
