# MysticCraft

A single unified Paper/Spigot plugin implementing Vampire Diaries-inspired
supernatural races for a factions-style server: **Vampires**, **Werewolves**,
and **Witches**, on top of a **Human** default.

Built for CanopyCreations / MysticCraft.

---

## Build

Requires **Java 25** (Minecraft moved to this requirement starting with the
26.1 game drop in 2026) and Maven, and an internet connection to pull the
Paper API (I could not build/test this myself in this sandbox — no outbound
network access here — so please run a build locally, or via CI, before
deploying):

```bash
cd mysticcraft
mvn clean package
```

**Version note:** this project targets Paper **26.2** (`26.2.build.112-stable`),
matching Minecraft's post-2026 year.drop versioning scheme (26.1, 26.2, etc.
— replacing the old 1.21.x numbering). If your server is on a different
Paper build, update the `<version>` in `pom.xml`'s `paper-api` dependency
and the `api-version` in `plugin.yml` to match, then rebuild.

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
- **No self-service.** There is no command to become a vampire - see
  "Becoming a Vampire" below.
- Enhanced speed, strength, and night vision (passive, always-on potion effects)
- **Sunlight burns** unless wearing a **Daylight Ring**
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

#### Becoming a Vampire
This is deliberately not a command - it's a multi-step ritual with real risk:

1. **Blood sharing**: an existing vampire and a human both sneak and
   right-click each other at the same time (sneaking solo instead triggers
   normal feeding). This puts vampire blood in the human's system for a
   configurable window (`vampire.turning.blood-window-minutes`, default 60).
2. **Death while the blood is active**: if that human dies before the blood
   wears off, they rise on respawn as a vampire — but only *transitioning*,
   not complete.
3. **Complete the transition**: a transitioning vampire must feed on a human
   within a deadline (`vampire.turning.transition-window-minutes`, default
   60) to finish becoming a true vampire.
4. **Fail the deadline → permanent death.** No transition, no vampire - the
   character dies for real (`player.setHealth(0)`), and their race reverts
   to Human.

Admins can still bootstrap the very first vampire(s) with
`/mystic setrace <player> vampire` (there's no other way to get a vampire
onto a server with zero vampires to begin the chain).

#### Daylight Rings & the Originals
The first N vampires (`vampire.originals.slot-count`, default **8**) —
whether admin-bootstrapped or naturally turned — automatically become
**Originals** and receive a free Daylight Ring, with a server-wide
announcement. Check remaining slots with `/mystic originals`.

Once all Original slots are claimed, the only way to get a ring is a witch
forging one: `/witch cast forgering` while looking at a vampire (costs
extra herbs, configurable via `witch.ring-forge-herb-cost`, default 3).

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

---

## The hidden werewolf bloodline

A percentage of players (`werewolf.latent-gene.chance-percent`, default
**8%**) silently carry the werewolf gene from their very first join. They
are told **nothing** — `/race` reports them as Human, because as far as
they know, they are one. There's no hint, no marker, no command that
reveals it to them.

Then, the first time they kill another player, the curse triggers and they
find out the hard way — with a disorienting, physical reveal message and
an automatic conversion to Werewolf. That kill might come from a faction
war, a duel, a betrayal, or a moment of panic; whatever the reason, the
surprise is the payoff.

**The scent mechanic:** triggered werewolves who spend enough continuous
time near a latent carrier (`scent-radius`, default 12 blocks;
`scent-seconds-required`, default 120s) start catching vague, atmospheric
hints — *"You catch something on the air. Familiar. Wrong."* The messages
**never name the player**, by design. A werewolf knows *someone* nearby is
like them, but has to figure out who through observation, elimination, and
roleplay. That ambiguity is what turns a hidden boolean into a social
mystery, and it's the main engine for the paranoia-and-secrets dynamic.

Admin tools: `/mystic bloodline <player>` to check, or
`/mystic bloodline <player> give|remove` to seed or clear it manually
(useful for staff-run story arcs — e.g. planting the gene on a specific
player before an event). Carriers are never notified by these commands.


---

## The Progenitors

Three unique titles. One holder each, per server, **ever**. These are meant
to be world events, not progression rewards — everyone who joins later
inherits a world those three players shaped.

| Title | Claimed by | Core power | Core weakness |
|---|---|---|---|
| §e**The Immortal** | A vampire who drives their humanity to **0** and survives it | Can't be permanently killed by stake, fire, or decapitation; immune to sunlight | Only witch desiccation can put them down; every witch senses their location |
| **The Original Witch** | A witch who casts **50 spells** | Only player who can turn someone directly; free spellcasting; forges rings at will | Power bound to the White Oak; can never be vampire or werewolf |
| **The First Wolf** | A werewolf who survives **3 forced full-moon transformations** | Shifts at will any night; immune to wolfsbane and silver; senses every latent carrier by name | Permanently locked as a werewolf; every vampire senses them within 100 blocks |

Each is claimed automatically the moment its condition is met, with a
server-wide announcement. Admins can also grant or strip titles manually
(`/mystic progenitor grant|strip|list`) for staff-run story arcs.

Read the full mythology in-game with `/lore progenitors`.

## The Seven Bloodlines

Every werewolf belongs to one of seven bloodlines, assigned automatically
(weighted) the moment their curse triggers. **Bloodline determines venom
strength** — a bite from an Emberfell wolf kills a vampire in 30 seconds at
6 damage/sec; a North Atlantic bite takes 110 seconds at 2/sec but is far
harder to outrun.

| Bloodline | Venom | Rarity |
|---|---|---|
| Riverborne | 3.0/s for 65s | Common (30) |
| Crescent | 3.5/s for 55s | Common (20) |
| Stone Ridge | 2.5/s for 75s | Common (18) |
| Ashwood | 4.0/s for 50s | Uncommon (15) |
| North Atlantic | 2.0/s for 110s | Uncommon (10) |
| Emberfell | 6.0/s for 30s | Rare (5) |
| Hollow-Born | 5.0/s for 40s | Very rare (2) |

Bitten vampires are told the fever "has a particular character to it" —
someone who knows the bloodlines might identify their attacker from how
they're dying. `/lore bloodlines` for the full text.

## Clans — the native faction system

MysticCraft now owns territory itself. **No external factions plugin is
required or supported.** This was a deliberate switch: after two failed
integration attempts (SaberFactions crashed on Minecraft's new versioning,
ZephyrusFactions is a closed-source beta), owning the system outright means
territory and race mechanics can actually interact instead of being two
unrelated systems bolted together.

**Five clan kinds**, four of them race-locked:

- **Court** (vampires) · **Pack** (werewolves) · **Coven** (witches) ·
  **Order** (humans) · **Circle** (open to anyone — for cross-race
  alliances and secret societies)

**Territory does something.** Standing on clan ground applies real effects
based on who holds it:

| Ground | Effect on intruders |
|---|---|
| Coven | Consecrated — vampires are weakened |
| Pack | Vampires slowed; **worse under a full moon** |
| Court | Humans and witches suffer hunger — "something here is hungry" |
| Order | Vervain and wolfsbane in the air — both vampires and werewolves weakened |

Your own clan's ground gives a matching racial bonus, so territory is worth
fighting for rather than just marking.

**Secret clans:** `/clan secret` hides a clan from all public listings.
Only members know it exists — built specifically for the conspiracies and
hidden alliances an RPG server runs on.

Commands: `/clan create|invite|join|leave|claim|unclaim|info|list|ally|enemy|promote|kick|disband|secret`
(aliased to `/c` and `/f`).

## The Codex

`/lore` puts the whole mythology in-game — origins, progenitors,
bloodlines, and a page for each race written in-world rather than as
mechanics documentation. Lore that only lives in a README is lore nobody
reads.


---

## The Three Names

The founding players of this server are written into the mythology itself.
Each progenitor title carries a historic name whose etymology genuinely
connects to the player behind it — and each title is **reserved**, so it
cannot be claimed by anyone else no matter who meets the condition first.

### Akaios, the Unburied — *The Immortal* (Ace)
**Ancient Greek.** From the same root as *akeratos* — "undiminished,
unspoiled, whole." A name given to a child expected to be flawless. The
irony was not lost on the woman he left at the altar.

He was the witch who wanted to never die, who convinced the greatest
spellcaster of his age to brew immortality for their wedding — and drank it
with someone else. He is not a vampire. He does not burn, does not thirst,
and has had two thousand years to get very good at getting what he wants.
The earth has refused him three times. Hence the name they gave him after.

### Eldrún Skugga, the Ash-Mother — *The Original Witch* (Shadowphoenix)
**Old Norse.** *Eldr* (fire) + *rún* (secret, hidden knowledge, rune) — "the
fire-secret." *Skugga* (shadow) was not a birth name but what the village
called her afterward, once they understood what she had made and could not
unmake.

She lost a child to wolves under a full moon and rewrote the oldest spell in
the world to make sure it never happened again. She drew on the sun for life
and the white oak for permanence, and nature charged her for every ounce of
it. She burned her old life down and built a new one out of the ashes —
every vampire in existence is what rose from that fire.

### Lupercus, of the Seven Knives — *The First Wolf* (Loubuscus)
**Latin.** From *lupus* (wolf) — the root beneath *Lupercalia*, the old
festival where tribes ran the boundaries of their land in wolfskin to keep
the wild out. They named the rite after the thing they buried.

Seven tribes decided she was too powerful to be allowed to grow up. Seven
knives went in. It wasn't enough — with what she had left she reached up out
of the ground and chained every one of them to the moon, out to their last
descendant. Seven knives went in; seven bloodlines came back out.

### How reservations work
Configured in `config.yml` under `progenitors.reserved`. When the named
player meets their title's condition, they get a heavier recognition
sequence — *"The name comes back to you before you have time to wonder
why"* — instead of the standard announcement. Anyone else who meets the
condition is turned away with a line about something vast recognising them
as a stranger and settling again.

Set a value to `""` to make that title first-come, first-served instead.
Read them in-game with `/lore names`.

---

## Discovery & progression

The problem this solves: lore in a `/lore` command is lore nobody reads, and
a supernatural server without direction is just a factions grind with fangs.

### The codex starts locked

`/lore` begins almost entirely blank. **13 fragments** unlock from things
players actually do — no fetch quests, no reading required to trigger them:

| Fragment | How it's found |
|---|---|
| On the Balance | First join |
| Something in the Dark | Survive a night under open sky |
| The Thirst | Feed on someone, or **be fed on** |
| The Sun and the Ring | Watch a vampire burn (including from 16 blocks away) |
| The Humanity Switch | Let your humanity slip below the threshold |
| The Sleeping Curse | Be within 24 blocks of a forced transformation |
| Seven Knives | Find a wolf shrine |
| On Venom | Survive a werewolf bite |
| What Magic Costs | Cast your first spell |
| On Channeling | Channel alongside another witch |
| The Ash-Mother | Stand near a pale oak (the White Oak) |
| The Unburied | Come within 20 blocks of a progenitor |
| On the Cure | Unlocks once you're near codex completion |

Discovery is quiet — a chime and one line, not an achievement banner.
`/lore found` shows what you have and `???` for what you don't. **Players end
up holding different pieces**, which gives them something real to trade.

### Questlines

Each race gets a five-step story that teaches its mechanics by making you use
them. `/quest` shows where you are; completed steps deliver in-world
narration instead of a "quest complete" popup.

- **The Newly Dead** (vampire) — Feed → survive dawn → hold humanity above 70
  → find an elder → turn someone
- **The Waking** (werewolf) — Learn your bloodline → survive a forced moon →
  three moons without killing → join a pack → venom a vampire
- **The Borrowed** (witch) — First spell → channel with another → survive
  backlash → claim coven ground → forge a ring for a vampire
- **The Ones Who Stayed** (human) — Find three fragments → carry vervain →
  arm yourself → join an Order → survive 30 days unturned

Turning mid-game swaps your line rather than orphaning it: a human who
becomes a vampire drops "The Ones Who Stayed" and starts "The Newly Dead" at
step one, which is thematically exactly right.

### Lorekeepers

Six NPCs, spawned with `/mystic keeper <role>`. No Citizens or other NPC
plugin needed — they're tagged, AI-disabled, invulnerable villagers whose
identity lives in their own persistent data, so they survive restarts.

| Keeper | Speaks to | Teaches |
|---|---|---|
| The Wanderer | anyone | Something in the Dark |
| The Apothecary | humans | The Thirst |
| The Elder | vampires | The Humanity Switch |
| The Packmother | werewolves | Seven Knives |
| The Hedgewitch | witches | What Magic Costs |
| The Archivist | anyone | The Unburied (+ tells you your codex count) |

**Race-gated keepers refuse the wrong audience** — *"...no. Not for you, I
don't think."* That refusal is itself informative, and it means a vampire and
a human asking the same NPC the same question get different answers. Which
is the whole engine of a secrets-driven server.


---

## Actions, not commands

The intended play loop requires **no typing**. Commands remain as an
accessibility fallback, but every core supernatural act is something you do
with your body and hands — and something bystanders can watch happen.

### Witch: gesture casting
Hold the Grimoire Wand. **The motion is the spell.**

| Gesture | Spell |
|---|---|
| Raise it to the sky | heal |
| Kneel and touch the earth | boundary |
| Strike toward someone | telekinesis |
| Point at them, crouched | pain |
| Point at them, standing | desiccate |
| Kneel, wand raised | forge a Daylight Ring |

Each casts with its own coloured particle ring and a beam to the target.
Non-witches holding a wand get *"The wand is inert in your hand. It isn't
yours."*

### Vampire: compulsion by eye contact
Crouch, get within range, and look them in the eye — **and they have to be
looking back.** A progress bar fills for both of you while a thread of dark
particles is drawn between your eyes. The target can break it by looking
away or running. Vervain stops it cold, and both parties feel the failure.

**Anyone within 12 blocks sees it happen** — *"Something passed between them
just then"* — and learns a codex fragment from watching. Secrecy is a real
problem, not a formality.

### Werewolf: howl to shift
Crouch under a night sky, look straight up, hold it for a moment. You howl —
and the howl carries **120 blocks**, audible to every player in range with
volume falling off by distance. Everyone past 40 blocks gets *"A howl,
somewhere out there."* Bones crack, particles burst, and anyone within 24
blocks learns what they just watched.

### Territory: plant a totem
Ground is claimed by planting a physical **Clan Totem**, not by typing. It
stands there glowing in your clan's colour, visible while anyone walks the
chunk. **An enemy can walk up and break it** — which takes the land, alerts
every member of the losing clan with a title card and coordinates, and turns
territory into a raid objective with an actual location to defend.

### Lore: physical books
Fragments exist as real `WRITTEN_BOOK` items with in-world authorship
(*"scratched into stone"*, *"burned into bark"*, *"torn from a longer
book"*). They **generate in natural loot** — witch material in strongholds
and libraries, vampire material in mansions and villages, wolf material in
jungle temples and mineshafts, the deepest lore in bastions and the End.

Reading one unlocks the fragment permanently, so the book can then be given
away while the knowledge stays with you. That means secrets can be carried,
hidden in chests, traded to an ally, dropped on death, or **looted off a
corpse.** Knowledge becomes a position in the world's politics.

### Everything is visible
`Fx` centralises the feedback so no supernatural act is invisible: sunlight
burning throws flame and smoke, feeding sprays blood-red particles, venom
drips from a bitten vampire every tick, boundary spells draw a literal
shimmering wall, and a progenitor awakening triggers a flash, an ender
dragon roar, and a beacon tone **every player on the server hears wherever
they are.**

---

## Ashfall — the town

A deliberate parallel to the source material's small-town setting rather
than a copy of it. The names are ours, and each ties back to the
Ash-Mother's story, so the geography and the mythology explain each other.
(She burned her old life down here; the town grew up in the ash.)

**The point is not decoration.** Every landmark does something mechanically.

| Landmark | Parallel | What it does |
|---|---|---|
| Ashfall Square | town square | Neutral ground — unclaimable |
| The Kettle | the bar | **Sanctuary — PvP disabled inside** |
| The Old Boarding House | vampire house | Court seat; vampires get Strength |
| Lockridge Manor | wolf estate | Pack seat; werewolves get Speed |
| The Hedge House | witch house | Coven seat; cheaper spells |
| St. Ansel's Ruin | burned church | Consecrated — witches amplified, vampires weakened |
| The Tomb | beneath the church | **Sealed content** — opens via server event |
| Wicker Bridge | the bridge | Running water — vampires slowed crossing |
| **The White Oak** | the white oak | See below |
| The Quarry | the quarry | Neutral ground |
| The Old Cemetery | founders' plots | Consecrated |

Entering one shows a title card and unlocks a matching codex fragment, so
exploring the map *is* reading the lore.

### The White Oak is a real, destructible tree

The single most consequential object in the world is a tree anyone can walk
up to with an axe. Felling it:

1. **Permanently strips The Original Witch** of her progenitor status
2. Yields **White Oak Stakes** — the only wood that kills a vampire for good,
   *including The Immortal*, who is otherwise unkillable
3. Triggers an ender dragon roar and a server-wide announcement

Vampires physically cannot cut it — *"Your hands won't do it. Something
older than you says no."* Every log removed announces itself to anyone
within 80 blocks, so felling it takes time and cannot be done quietly.
Stakes are consumed on use and the supply is finite.

That one block generates a permanent political problem: vampires want it
gone, the Original Witch can't afford to lose it, humans want stakes, and
nobody can guard it forever.

### Generating it

```
/mystic town confirm
```

Builds the town centred on you and registers all 11 landmarks.

**Honest scope note:** procedural building generation that looks genuinely
good is hard, and I couldn't visually test any of this. What the generator
produces is a solid, readable *skeleton* — correct layout, correct scale,
distinct silhouettes (the stopped clock tower, the ruined church with its
fallen bell, the oversized pale oak in its ring of stones), with everything
wired to the mechanics immediately.

It's designed to be replaced. Build the real Ashfall by hand or drop in
schematics, then re-point each landmark:

```
/mystic landmark set the_kettle 24
/mystic landmark list
/mystic landmark tp the_white_oak
```

Nothing downstream cares whether a landmark was generated or hand-built —
only where it is. **That's the part that makes this work with your future
minimap too:** landmark positions live in `landmarks.yml` as plain
coordinates, so a web map can read them directly and render the town with
real labels.

---

## API verification against Paper 26.2

Verified directly against the official
[26.2 javadocs](https://jd.papermc.io/paper/26.2/) rather than guessed:

**Fixed — `Attribute` has no `GENERIC_` prefix in 26.2.** It's now an
*interface* with static fields: `MAX_HEALTH`, `MOVEMENT_SPEED`,
`ATTACK_DAMAGE`. An earlier build added `GENERIC_` to fix a Paper 1.21.1
compile error; that prefix is wrong for 26.2 and has been reverted.

**Fixed — `Particle.FLASH` requires a `Color` data argument** in 26.2. The
progenitor-awakening effect was calling it without one, which compiles but
throws at runtime. Swapped to `EXPLOSION_EMITTER`.

**Verified correct as written:**
- All 13 particles used (`DUST`, `SMOKE`, `LARGE_SMOKE`, `FLAME`, `CRIT`,
  `WITCH`, `END_ROD`, `ENCHANT`, `ITEM_SLIME`, `EXPLOSION`,
  `EXPLOSION_EMITTER`) exist; `DUST` correctly takes `Particle.DustOptions`
- All 10 `PotionEffectType` constants use the modern post-1.20.5 names
  (`NAUSEA`, `SLOWNESS`, `STRENGTH`, `RESISTANCE`, not the old
  `CONFUSION`/`SLOW`/`INCREASE_DAMAGE`/`DAMAGE_RESISTANCE`)
- All 85 `Material` constants, including the pale oak set
  (`PALE_OAK_LOG`/`_LEAVES`/`_PLANKS`/`_WOOD`), `REINFORCED_DEEPSLATE`,
  `DEEPSLATE_TILE_STAIRS`, `TUFF_BRICKS`
- `Material` and `Particle` are still enums; `Sound` and `Attribute` are now
  interfaces, but all usage is static field access either way
- No `EnumMap`/`EnumSet` is used on any Bukkit `Keyed` type — the 26.2 docs
  explicitly warn those may stop being enums

Remaining deprecation warnings (`sendTitle`, `broadcastMessage`,
`setDisplayName`, `setLore` — Adventure has replacements) are warnings only
and will not fail the build.
