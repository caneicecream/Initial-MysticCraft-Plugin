# MysticCraft

A single unified Paper/Spigot plugin implementing Vampire Diaries-inspired
supernatural races for a factions-style server: **Vampires**, **Werewolves**,
and **Witches**, on top of a **Human** default.

Built for CanopyCreations / MysticCraft.

---

## Build

Requires Java 21 and Maven, and an internet connection to pull the Paper API
(I could not build/test this myself in this sandbox — no outbound network
access here — so please run a build locally before deploying):

```bash
cd mysticcraft
mvn clean package
```

The output jar will be at `target/mysticcraft-1.0.0.jar`. Drop it in your
server's `plugins/` folder.

**A note on honesty:** I wrote this carefully against the Paper 1.21.1 API
and it *should* compile clean, but I have not been able to actually run
`mvn package` against it in this environment. The one area most likely to
need a tweak across different Minecraft versions is the `Attribute` enum
names in `RaceManager.java` (Mojang/Paper has renamed attributes a couple
times across 1.20–1.21). If it doesn't compile out of the box, that's the
first place to look — paste the compiler error into a Claude Code / Claude.ai
chat and it'll be a quick fix.

---

## Races & mechanics implemented

### Vampire
- Enhanced speed, strength, and night vision (passive, always-on potion effects)
- **Sunlight burns** unless wearing a **Daylight Ring** (`/mystic item ring`)
- **Blood feeding**: sneak + right-click an entity with an empty hand to heal
  and drain them — costs humanity
- **Humanity meter** (0–100): drops from feeding; low humanity risks
  "ripper" rage spikes (random Strength/Nausea bursts)
- **Vervain**: weapon coated in it burns and weakens vampires on hit; worn by
  a target, it blocks vampire compulsion
- **Compulsion**: `/vampire compel <player> <suggestion>` — range + eye
  contact gated, blocked by vervain, on a cooldown
- **Permanent kills**: wooden stake to the heart, fire/lava, or decapitation
  (axe attacks) all instant-kill a vampire

### Werewolf
- **Curse trigger**: a werewolf-blooded player is mechanically human until
  they kill another player — that death "triggers" the curse
- **Forced transformation** on real in-game full moons (Minecraft's natural
  8-day moon cycle), with no player choice involved — reverts at dawn
- Once triggered, players can also voluntarily `/werewolf shift` outside of
  a forced full moon
- Shifted werewolves get major Strength/Speed/Regen buffs
- **Wolfsbane** weapons burn and weaken werewolves
- **Silver** weapons deal bonus damage to werewolves
- A shifted werewolf's bite injects **venom** that's lethal to vampires over
  time unless cured (`/mystic cure <player>`, or extend with a "werewolf
  blood" cure item)

### Witch
- Spellcasting via `/witch cast <spell>`, gated by a consumable **Spell
  Herb**, on a cooldown
- **Channeling**: nearby witches boost your spell power (with a real
  overchanneling backlash risk if you push it — TVD-accurate)
- Spells included: `heal`, `telekinesis`, `pain` (aneurysm-style), `boundary`
  (temporary anti-vampire barrier), `desiccate` (vampire-specific drain)

### Shared
- `/race info|set|list|humanity` for all players
- `/mystic` admin command: force-set races, adjust humanity, give lore
  items, check moon phase, cure venom, reload config
- All balance numbers (damage, durations, cooldowns, percentages) live in
  `config.yml` — nothing is hardcoded

---

## Factions integration

MysticCraft ships with an optional bridge to **SaberFactions** (see
`integrations/factions/FactionsBridge.java`). It only activates if it
detects a plugin named `Factions` on your server — if you're not running
one yet, or you install something incompatible later, MysticCraft logs a
warning and keeps working normally; the bridge just turns itself off.

**What it does:**
- **Auto-assigns faction membership on turning** — turn someone into a
  Vampire and they're automatically moved into whatever faction you've
  mapped to `VAMPIRE` in config (default: a faction tagged `Vampires`).
  You need to actually create that faction first (`/f create Vampires`).
- **Race-locks faction membership** — blocks `/f join` into a mapped
  faction if the joining player's race doesn't match.
- **Race-majority faction buff** — if 70%+ (configurable) of a mapped
  faction's online members share the dominant race, they get a small
  passive Regeneration buff, every 30 seconds.

Configure the race → faction tag mapping in `config.yml` under
`factions-bridge.race-factions`.

**Honesty check on this part specifically:** SaberFactions has changed
hands a few times (TeamPixel → SaberLLC → the current JitPack build under
Driftay), and Factions-plugin APIs across the wider ecosystem have drifted
a lot over the years — the "official" FactionsUUID, for instance, has
moved to a completely different, newer API under a different package name
as of its 4.x releases. I built this against the classic
`com.massivecraft.factions` API that SaberFactions' own repo confirms it
kept, and wrapped every bridge call in a try/catch so a mismatch degrades
gracefully instead of crashing MysticCraft — but I could not compile or
test this against a live SaberFactions jar in this environment. If a
method name doesn't line up when you build it, the fix is small and
localized to `FactionsBridge.java` (I left comments there pointing at the
exact methods to double-check first).

---

## Commands

| Command | Who | Purpose |
|---|---|---|
| `/race [set\|info\|humanity\|list]` | everyone | manage your own race |
| `/vampire compel <player> <text>` | vampires | compulsion |
| `/vampire status` | vampires | humanity/ring/venom status |
| `/werewolf shift` | werewolves | manual shift (post-trigger) |
| `/werewolf info` | werewolves | curse/shift/moon status |
| `/witch cast <spell>` | witches | cast a spell at your crosshair target |
| `/witch list` | witches | list known spells |
| `/mystic ...` | op / `mysticcraft.admin` | admin tools |

---

## What's deliberately scoped out of v1 (and how I'd add it next)

TVD's *full* mythos is genuinely enormous — Originals, hybrids, sirelines,
the Gilbert ring, the Five/Hunter's Curse, the moonstone/doppelganger
sacrifice, witch ancestral magic tied to specific bloodlines, the Other
Side, etc. Building all of that "100% accurate" in one pass would be a
multi-month project even for a paid commercial plugin team, and cramming it
in unreviewed would almost certainly ship broken. Instead I built a solid,
correctly-architected core (race system, attribute sync, persistence,
config-driven balance, all three race's signature mechanics) that's
structured so each of those can be layered on cleanly:

- **Originals / sirelines**: add a `sireLine` field to `PlayerData`, then
  gate a vampire's "death" through a `checkSireLine()` hook before applying
  damage.
- **Hybrids**: just a `Race.HYBRID` entry combining `RaceManager`'s vampire
  and werewolf attribute blocks.
- **Gilbert ring / resurrection**: a new lore item + a `PlayerDeathEvent`
  listener that cancels death once and teleports the player back.
- **Ancestral witch magic**: extend `SpellManager` with a `bloodline` string
  on `PlayerData` and gate certain spells behind it.
- **Moonstone / doppelganger ritual**: a scripted multi-step quest using
  existing item + location-check patterns already in `BoundaryManager`.

Happy to build any of these out next — just say which one.
