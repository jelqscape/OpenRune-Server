# Thieving — dev progress tracker

Not a shipped doc — dev-only tracking, same as `special-attacks/PROGRESS.md` was. Will be
deleted before this goes into a PR, same as that one was.

## Scope

Working from a 3-tier plan agreed with the user:

- **Tier 1** (in progress): pickpocketing, stalls, chests, pickable doors — the base 1-99 skill.
- **Tier 2** (not started): Pyramid Plunder, Blackjacking, Rogues' Den, Sorceress's Garden.
- **Tier 3** (not started, likely out of scope for rev 240): Ardougne Knights, Kourend artefact
  stealing, Dorgesh-Kaan rich chests, Rogues' Castle chest, and anything past that.

## Tier 1 status

| Piece | Status |
|---|---|
| Pickpocketing | In progress — see below |
| Stalls | Not started |
| Chests | Not started |
| Pickable doors | Not started |

## Pickpocketing — what's real

- **Success formula**: uses the codebase's existing shared `SkillingSuccessRate`/`statRandom` (same
  one Fishing/Mining/Woodcutting use), not a duplicate. Verified this formula has a `+1` baked in
  that isn't obvious from the wiki's own stated fractions — `lowChance`/`highChance` in
  `PickpocketingData.kt` are the wiki's `low/256`/`high/256` values **minus 1** to compensate; this
  is documented in the file and covered by tests that assert the formula's output matches the wiki
  chart exactly, not approximately.
- **Stun**: real two-part mechanic (`ThievingStun.kt`) — 9-tick move-lock (fixed, wiki-stated) +
  per-NPC retry-lock (mostly 8 ticks, a couple of NPCs differ). Move-lock reuses the engine's
  `frozen` flag with its own timer, doesn't touch magic-freeze immunity.
- **Coin pouches**: real, working. Discovered dynamically by cache category (not hardcoded item
  names), Open/Open-all both wired. Had a real bug where discovery scanned
  `ServerCacheManager.getObjects()` (world scenery/`ObjectServerType`, a different cache entirely)
  instead of `getItemTypes()` - compiled fine since both types happen to expose
  `category`/`internalName`, but it meant zero pouches ever got a handler and every one produced
  the engine's generic "Nothing interesting happens." Fixed at the source.
- **Rewards**: reworked once already - the wiki's tables are *mutually exclusive* outcomes (a
  successful Rogue pickpocket gives coins **or** a rune **or** a lockpick, never both), not "always
  give the coin pouch, then separately roll a bonus item" like the first pass had it. Fixed in
  `PickpocketingEvents.giveRewards`: NPCs with no `dropTableId` always give their coin pouch (real
  "coins, always" NPCs); NPCs with a `dropTableId` roll the table as the *entire* reward, coin pouch
  included as one of its own rows.
- **Rocky pet**: rolls on every successful pickpocket (`RockyPet.kt`, mirrors Fishing's
  `HeronPet.kt` pattern exactly). Rate `1/(176743 - level*25)`, wiki-cited (Mod Roq) and identical
  across every NPC's own page.
- **No auto-repeat**: one click = one attempt, matching real OSRS - an earlier pass had this
  auto-repeating on every success, which turns out to be a Larcenist-league-only passive
  ("automatically re-pickpocket an NPC"), not base-game behavior. Removed.
- **Success delay**: a successful pickpocket has no move-lock, but still can't be immediately
  retried - the wiki's own averaged-ticks formula for the skill ("every pickpocket will take on
  average 2 + 8(1-p) ticks", p = success chance) implies a flat 2-tick minimum even when nothing
  goes wrong. Implemented via `ThievingStun.applyRetryLockOnly`, reusing the same retry-lock check
  a failed attempt uses, just without the freeze.
- **Stun visual**: `spotanim.stunned_thieving` (a real, dedicated gameval for exactly this) now
  plays above the player's head when a failed attempt triggers the stun.
- **Audit tooling**: `::thieveaudit [attemptsPerNpc]` sweeps every NPC below, one spawn-attempt-
  despawn cycle each; `::thieveaudit <id> [attempts]` drills into one. Paces a `delay(2)` after
  every individual attempt (not just once per NPC) so a failed attempt's 1-tick-delayed damage hit
  fully resolves before the next attempt - otherwise multiple NPCs' hits could land on the same
  tick and pile up into a confusing stack of hitsplats.
- **Tests**: `PickpocketingDataTest.kt` regression-pins every NPC's numbers and the formula
  cross-check. No integration/live-server tests yet (the `integration-test-suite` convention exists
  in this repo but isn't wired into this module's build file). No tests on the drop tables
  themselves yet (weight sums, item names) - worth adding.

### NPC roster (17 wired)

| NPC | Lvl | Bonus loot table? | Notes |
|---|---|---|---|
| citizen (Man/Woman) | 1 | No (coins only, wiki-confirmed) | |
| farmer | 10 | **Yes** (coin pouch or potato seed) | |
| ham_member | 15 | No | Real table exists (weapons/ores/herbs/H.A.M. robes/easy clue) but is unusually large and messy on the wiki page - deferred rather than guess at item names |
| warrior | 25 | No (coins only, wiki-confirmed) | |
| rogue | 32 | **Yes** (coin pouch, air runes, jug of wine, lockpick, or iron dagger(p)) | |
| master_farmer | 38 | **Yes** (allotment seeds only, no coin pouch of its own) | Wiki also documents hop/flower/bush/special/herb sub-tables - not included |
| guard | 40 | No (coins only, wiki-confirmed) | |
| desert_bandit | 53 | **Yes** (coin pouch, antipoison(1), or lockpick) | |
| knight_of_ardougne | 55 | No (coins only, wiki-confirmed) | |
| watchman | 65 | **Yes** (coin pouch AND bread, both guaranteed - not a roll) | |
| paladin | 70 | **Yes** (coin pouch AND 2 chaos runes, both guaranteed) | Missing: rare hard clue scroll |
| gnome | 75 | **Yes** (arrow shafts, coin pouch, swamp toad, gold ore, earth rune, king worm, or fire orb) | Missing: uncommon medium clue scroll |
| hero | 80 | **Yes** (coin pouch, death runes, jug of wine, blood rune, fire orb, diamond, or gold ore) | Missing: very rare elite clue scroll; rogue-equipment doubled-reward variant not implemented |
| wealthy_citizen | 50 | **Yes** (coin pouch or Varlamore thieving house key) | Missing: easy clue scroll; street-urchin 100%-success distraction event not implemented |
| vyre | 82 | **Yes** (coin pouch, death runes, blood pint, uncut ruby, blood runes, diamond, or cooked mystery meat) | Missing: 1/5,000 blood shard roll; quest-gated (Sins of the Father) not enforced |
| elf | 85 | **Yes** (coin pouch, death runes, jug of wine, nature runes, fire orb, diamond, or gold ore) | Lletya-era 7-row table only; Prifddinas pre-roll (crystal shard, enhanced crystal teleport seed) not included; quest-gated (Mourning's End Part I) not enforced |
| tzhaar_hur | 90 | **Yes** (Tokkul, or an uncut sapphire/emerald/ruby/diamond) | No coin pouch (real OSRS behavior). Ice-gloves burn mechanic implemented: 4 typeless damage on every successful pickpocket unless wearing ice gloves (failure damage was already unconditional) |

**Excluded, not forgotten**: Cave goblin and Fremennik citizen have no success-chance data on the
wiki at all (checked twice). Villager only works via blackjack post-quest, a different mechanic
this system doesn't implement, and its plain-pickpocket XP is 0 anyway.

**Clue scrolls are omitted from every table above.** This cache has no single generic "clue scroll
(tier)" item beyond `obj.trail_clue_beginner` - easy/medium/hard/elite are each dozens of
individually-named variants (`trail_clue_easy_emote001`, etc.) with no random-variant-picker built
yet. Their probability share is folded into the surrounding weights or "nothing" for now. Building
a proper `randomClueOfTier(tier)` helper is real, valuable follow-up work - Paladin/Gnome/
Hero/Wealthy citizen all canonically drop one.

## Known gaps across the board

- No item/equipment modifiers: Thieving cape (+10%), gloves of silence (+5%), Ardougne
  Diary (+10% in/all Ardougne), Dodgy necklace (25% stun-avoid, 10 charges), Shadow Veil
  (15% stun-avoid). All wiki-verified numbers from the first research pass, just not wired up.
  See `Thieving` wiki page § Equipment.
- No Rogue outfit double-loot bonus (several tables above have a documented "double with rogue
  equipment" variant that's simplified to the base amount).
- No quest-requirement gating anywhere (H.A.M. member, Vyre, Elf, TzHaar-Hur inner city all have
  real prerequisites in OSRS that aren't checked here).
- No clue scroll random-variant picker (see above) - affects Paladin/Gnome/Hero/Wealthy citizen.
- H.A.M. Member's real bonus table is large and wasn't confidently transcribable from the wiki
  page's structure - still coin-pouch-only.

## Next up

1. Stalls (Tier 1) - mechanically simple (no roll at all, just LOS/guard-aggro + a shared 10-min
   per-owner-type cooldown), full stall table already researched in an earlier pass.
2. Chests, then doors.
3. Build the clue-scroll random-variant picker and wire it into Paladin/Gnome/Hero/Wealthy citizen.
4. Revisit H.A.M. Member's full table with a more careful pass.
