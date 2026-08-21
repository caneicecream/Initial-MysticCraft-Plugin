# MysticCraft — server stack notes

Notes on the plugins running alongside MysticCraft, what conflicts exist,
and what's worth building next.

---

## Install this first: PlaceholderAPI

**It's missing from your server, and both HUDEngine and CommandPanels need
it.** Both list it as a soft dependency, and neither can read MysticCraft
data without it.

```
/papi ecloud download <expansion>   # not needed for ours - we register directly
```

Just drop PlaceholderAPI in `plugins/` and restart. MysticCraft registers
its own expansion automatically; there's nothing to download for it.

---

## Placeholders

Every one of these works in HUDEngine HUDs, CommandPanels GUIs,
EssentialsChat prefixes, scoreboard plugins — anywhere placeholders are read.

### Race
| Placeholder | Returns |
|---|---|
| `%mysticcraft_race%` | `Vampire` |
| `%mysticcraft_race_colored%` | `§4Vampire` |
| `%mysticcraft_race_color%` | just the colour code |
| `%mysticcraft_is_vampire%` | `true` / `false` (also `_werewolf`, `_witch`, `_human`) |

### Vampire
| Placeholder | Returns |
|---|---|
| `%mysticcraft_humanity%` | `74` |
| `%mysticcraft_humanity_bar%` | a 10-segment text meter |
| `%mysticcraft_is_ripper%` | `true` below the humanity threshold |
| `%mysticcraft_has_ring%` / `%mysticcraft_ring_status%` | daylight ring state |
| `%mysticcraft_burning%` | `true` while actually taking sun damage |
| `%mysticcraft_is_transitioning%` | mid-turning |
| `%mysticcraft_transition_minutes%` | minutes left to feed or die |
| `%mysticcraft_is_original%` | one of the eight Originals |

### Werewolf
| Placeholder | Returns |
|---|---|
| `%mysticcraft_bloodline%` / `_colored%` | `Emberfell` |
| `%mysticcraft_curse_triggered%` | has the curse woken |
| `%mysticcraft_is_shifted%` / `%mysticcraft_shift_status%` | current form |
| `%mysticcraft_venom_damage%` | their bloodline's venom rate |

### Moon
`%mysticcraft_full_moon%` · `%mysticcraft_moon_status%` · `%mysticcraft_is_night%`

### Witch
`%mysticcraft_spell_cooldown%` · `%mysticcraft_spell_ready%` · `%mysticcraft_spells_cast%`

### Progenitor
`%mysticcraft_progenitor%` · `_colored%` · `_name%` (the historic name) · `%mysticcraft_is_progenitor%`

### Clan & territory
`%mysticcraft_clan%` · `_colored%` · `_kind%` · `_members%` · `_land%` · `_rank%`
`%mysticcraft_territory%` — whose ground you're standing on
`%mysticcraft_landmark%` — which landmark you're inside

### Codex & quests
`%mysticcraft_codex_progress%` (`7/13`) · `%mysticcraft_quest_line%` ·
`%mysticcraft_quest_step%` · `%mysticcraft_quest_instruction%`

---

## Two real gameplay conflicts in your current stack

### 1. EssentialsX `/home` breaks two core mechanics

This is the one worth thinking about hardest. Right now a vampire caught in
sunlight can `/home` to safety instantly, and a werewolf can teleport out of
a confrontation the moment the moon forces a shift. Both of those moments
are supposed to be the tense part.

Options, in rough order of how much I'd recommend them:

- **Add a teleport warmup** in Essentials config (`teleport-delay: 5`) and
  cancel on damage. A burning vampire can't escape, but ordinary travel
  still works. Least disruptive fix.
- **Block teleport while burning or shifted.** Needs a small listener in
  MysticCraft — say the word and I'll add it.
- **Remove `/home` entirely.** Purest, most annoying.

### 2. EssentialsX god mode overrides permanent kills

`/god` will cancel the stake, fire and white-oak instakills. That's correct
behaviour on Essentials' side, but worth knowing before you test — if a
staking "doesn't work," check god mode before assuming a bug.

### Smaller notes

- **MythicMobs** is logging warnings about `ExampleQuestAccepted` and
  similar. That's just its shipped example config referencing skills that
  don't exist. Harmless; delete the example files to quiet it.
- **Orebfuscator** hides ores from clients. It won't touch `PALE_OAK_LOG`,
  so the White Oak stays visible — worth confirming after any config change,
  since an invisible White Oak would be a bad day.
- **Multiverse-Inventories** separates inventories per world. MysticCraft's
  race data is deliberately *global* — you don't stop being a vampire by
  walking through a portal. That's the right behaviour, but be aware the two
  systems disagree philosophically.
- **VaultUnlocked** registers as `Vault` in `/pl`, which is why you see the
  old name. Nothing wrong.

---

## What to build next — an honest ranking

You asked about RPG mobs, custom enchants, and merging the popular factions
plugins into MysticCraft. All three are doable but they're very different
sizes, so here's my actual read:

**1. Race HUDs (small, high impact).** Now that placeholders exist this is
mostly config, not code. A vampire sees humanity and a sun warning, a
werewolf sees moon phase and shift state, a witch sees spell cooldown. This
makes mechanics that currently live in chat messages *legible*, which is the
single biggest quality-of-life gap right now.

**2. Race GUIs via CommandPanels (small).** `/race`, `/clan` and `/lore`
currently print walls of text. Panels driven by the same placeholders would
be a large presentation upgrade for very little work — and it's config, so
you can iterate without waiting on me.

**3. Custom enchants (medium).** Genuinely worth doing *if* they're
race-flavoured rather than generic — a blade that does more damage to the
undead, armour that resists compulsion, a stake that survives one use. Maybe
8–12 enchants tied to the existing lore.

**4. RPG mobs (medium, but MythicMobs already does it).** You have
MythicMobs installed and it's better at mob design than anything I'd write.
The valuable part isn't rebuilding it — it's **bridging** it: MythicMobs
mobs that drop lore fragments, spawn more on full moons, or only appear on
consecrated ground. That's a small integration rather than a whole system.

**5. Merging factions features (large — months).** Auctions, shops, koths,
crates, bounties, spawners, envoys. I'd genuinely push back on this one for
now: each is a real system, and building six mediocre versions of plugins
that already exist would cost more than it returns. Better to pick the two
or three that actually matter for a supernatural RP server — I'd argue
**bounties** and **KOTH-style contested territory** fit the war theme, and
the rest are generic grind features that don't serve your world.

My recommendation: do 1 and 2 now (both are quick), then 3, and treat 4 as a
bridge rather than a build.
