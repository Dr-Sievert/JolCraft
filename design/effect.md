# Effect: effect rules and damage affinity

Draft of 2026-11-10. Built on 2026-11-10 in steps 1 (the rules), 2 (cure and hold) and 4 (damage); step 3 (crowd
control) was built and removed again the same day, see "Removed 2026-11-10", and what this design says of it is kept as
the record for the features that own stun, root, suppress and disarm. From a scout of the old tree's effect and damage code (the stacking, protection,
duration, crowd-control and cure helpers, the owned-effect helper, the six affinity rows and their eighteen entity
tags, eight mixins) against vanilla 26.1.2 with NeoForge 26.1.2.122's patches; four scouts reported and the claims
this design leans on were re-read in the sources (two were wrong, see the end of "What 26.1.2 gives"). The author chose the
scope: the effect rules together with damage affinity, as generic mechanisms with no mod content. Three systems in
four build steps were planned; two systems stand. Challenged 2026-11-10: eight findings confirmed and folded in, see "Challenged".

## What the old tree did

Effects were rule tables written into handlers, keyed by the mod's own effects and attributes. Sorted by what
survives:

- **Generic, ported as data.** *Stacking with a cap*: on `MobEffectEvent.Added` a second application became
  `old.amp + added.amp + 1`, clamped by a per-effect cap, with the longer duration, by re-entering `addEffect` behind a
  per-entity guard (which also ran `Applicable` a second time on the merged instance, so a random protection was rolled
  twice). *Protections*: an effect is blocked when a protecting attribute (strength `clamp(value, 0, 1)` as a chance)
  or a protecting effect (strength 1) lists it, and a protecting effect may cleanse what it lists when it is added.
  *Duration scaling*: on `Added`, beneficial effects lengthened by an attribute, crowd-control effects shortened by
  another, and a shortening of 1 or more blocking the effect outright (a `instanceof` class check, one handler on
  `Applicable`). *Crowd control*: Stunned, Rooted, Suppressed and Disarmed, each rule written out in four places
  (a server tick, eleven player events, three client input events, four mob-goal mixins). *Cure*: an `EffectCure` name
  per effect class, curses and Moon Shield cured by nothing. *Owned infinite effects*: a source (an armour set,
  Luminance) holds an infinite effect while a condition is true, called every tick, and removes only the instance it
  added, even when a stronger finite effect has pushed it into the hidden chain; ownership was a flag in persistent
  data, and in `PlayerPersisted` for players. *Damage affinity*: six rows of a damage-type tag (with excluded tags), a
  resistance and a vulnerability attribute, and three entity-type tags (immune, resistant, vulnerable) that became
  transient attribute modifiers on join; at `LivingDamageEvent.Pre` the damage is multiplied by
  `(1 + vulnerability) * (1 - resistance)` for every matching row. *Stage order*: one helper fixed the order of the
  mod's damage handlers, four at `LivingIncomingDamageEvent`, two at `Pre`, three at `Post`.
- **Content, nothing to port.** Every concrete effect, attribute, tag value and number; the curse family (Frailty,
  Cursed Wound, the Vitality and Hex health maths, Delirium, Ataxia); Stone Skin, Corrosion, Anchor, Famine, Dwarven
  Rage; Moon Shield, Sunfire, Overheal, the heal increase, Luminance critical; the Slowness rescale (an effect's
  attribute modifier scaled by an attribute, one pair of them); `MobEffectsMixin` (baking the mod's attribute into
  vanilla's Fire Resistance); `DiggerItemMixin`.
- **Mechanisms that are one attribute's behaviour.** Armour penetration (an `ARMOR` reduction modifier scaled by the
  attacker's attribute), the projectile bonus (a flat add) and the two `LivingEntityMixin` wraps that stop vanilla's
  Fire Resistance effect and the five-fold freeze damage from overriding the attributes. They have nothing to
  configure but one attribute; see "Not in this step".
- **Now vanilla or NeoForge, no port.** The mixin accessor class (an access transformer is smaller); the freeze and
  fire entity-type mixins keep their reason only with the wraps above.

## What 26.1.2 gives

- `MobEffectInstance`: `duration`, `amplifier` and `hiddenEffect` are private and have no setters; the constructor
  taking a hidden effect is public, the one taking `Details` private. `update(takeOver)` keeps the stronger and pushes
  the weaker or shorter onto the hidden chain; an infinite instance is never shorter than a finite one, so a finite
  instance of a higher amplifier pushes it down the chain and a finite one of equal or lower amplifier is dropped.
  `withScaledDuration` and `mapDuration` copy or read and mutate nothing.
- `LivingEntity.addEffect(newEffect, source)`: `CommonHooks.canMobEffectBeApplied` (the `Applicable` event, a result
  `APPLY`, `DEFAULT` or `DO_NOT_APPLY`, not cancellable, one call for `addEffect` and `forceAddEffect`), then `Added`
  (not cancellable, carries the unmerged new instance and the nullable old one, posted before the map is touched),
  then either the new instance is stored itself or `old.update(new)` runs and `onEffectUpdated` refreshes the
  attributes from `old`. `forceAddEffect` posts `Applicable` only; its callers are the client packet handler and
  NeoForge. The instance given to `addEffect` is stored by the game when the effect is new, so every caller passes a
  fresh one, but a fresh one is not always a new application: `ServerPlayer.restoreFrom(old, true)` (an End-portal
  return, from `PlayerList.respawn`) runs `addEffect(new MobEffectInstance(effect))` for every active effect on a player
  not yet added to the level (`isAddedToLevel()` false), and loading from a save does not go through `addEffect`.
- `Remove` is cancellable, per instance, for `removeEffect` and for each instance of `removeAllEffects` (milk's and
  the totem's `ClearAllStatusEffectsConsumeEffect`, `/effect clear`); it carries no cause. `Expired` is cancellable.
  There is no `EffectCure`: `IMobEffectExtension` holds only `getSortOrder`, and nothing in `LivingEntity` mentions a
  cure.
- Data maps: `DataMapType.builder(id, registryKey, codec)` on any registry, registered on `RegisterDataMapTypesEvent`
  on the mod bus (one id per registry), files at `data/<type namespace>/data_maps/<registry>/<type path>.json` with
  entries by id or by `#tag`, resolved while the data loads, and read with `holder.getData(type)` or
  `registry.getDataMap(type)`. The builtin mob effect and attribute registries take them; not synced unless the type
  asks. Tags of the static registries (mob effect, item) are sent to clients.
- Damage, in `hurtServer`: `isInvulnerableTo`, the vanilla Fire Resistance check (1201), `LivingIncomingDamageEvent`
  (cancellable, `setAmount`, `addReductionModifier`), shield, the freeze multiplier (1224) and helmet, then
  `actuallyHurt`: armour, then mob effects and enchantments, then `LivingDamageEvent.Pre` (`getNewDamage`,
  `setNewDamage`, not negative, not lethal), then absorption, then health, then `Post` (read-only). The reduction stage
  `INNATE_RESISTANCE` is set only by the Witch, so a reduction function on it would never run: affinity cannot ride on
  it and runs at `Pre`, as the old tree did.
- Fire immunity is `Entity.fireImmune()`, public, reading `getType().fireImmune()` (1479, used at 535, 610, 616, 2594,
  2950); there is no event or tag for it.
- Input and use: `AttackEntityEvent`; `PlayerInteractEvent` `LeftClickBlock`, `RightClickBlock`, `RightClickItem`,
  `EntityInteract`, `EntityInteractSpecific`; `BreakBlockEvent` (the old `BlockEvent.BreakEvent`); and
  `LivingEntityUseItemEvent` `Start`, `Tick` and `Stop`, where cancelling `Stop` stops the item being told it was
  released (a drawn bow does not fire); all cancellable. `EntityTickEvent.Post` runs for every ticking entity, players
  included (`ServerLevel.tickNonPassenger`). On the client, `InputEvent.InteractionKeyMappingTriggered`
  (`isAttack`, `isUseItem`, `isPickBlock`, `setSwingHand`) and `CalculatePlayerTurnEvent.setMouseSensitivity`
  are as before; `MovementInputUpdateEvent` hands over a `ClientInput` whose `keyPresses` is a writable `Input`
  (`Input.EMPTY`) but whose `moveVector`, which `LocalPlayer.applyInput` reads for the walk and which
  `KeyboardInput.tick` fills before the event, is protected.
- Mob goals: `MeleeAttackGoal`, `RangedBowAttackGoal` and `RangedCrossbowAttackGoal` each hold `mob` and answer
  `canUse` and `canContinueToUse`; a goal that is refused by the first or the second is stopped by the selector through
  its own `stop()` (the bow's stops using the item; the crossbow's clears the target and, mid-charge, the charge and its
  projectiles, so only charge progress is lost and a finished crossbow keeps its bolt; the melee goal's keeps a survival
  target and stops navigation, so the mob stops pursuing but keeps its target). The crossbow's `canContinueToUse` calls
  `canUse`, so it posts twice per tick, harmlessly.
  `SpellcasterIllager.SpellcasterUseSpellGoal` is a protected inner class of the same shape that reaches its mob as
  `SpellcasterIllager.this`; the summon and attack goals call `super.canUse`, but `EvokerWololoSpellGoal` overrides
  `canUse` and `canContinueToUse` without it, so injects on the base class never run for it. `Tags.Items.MELEE_WEAPON_TOOLS` and `RANGED_WEAPON_TOOLS` and `ProjectileWeaponItem` exist.
- Attachments are copied on a non-death clone and only the `keptOnDeath` ones on death; effects follow the same rule,
  so an attachment tied to effects is saved and not kept on death.
- Access transformers: MDG picks up `src/main/resources/META-INF/accesstransformer.cfg` in development and FML finds
  the same path at runtime. Mixins need a `[[mixins]]` entry in the mods.toml template and a JSON config, as the
  game-test mod has.

Where the scouts were wrong: the constructor with a hidden effect is public, not private (a chain can be rebuilt but
not read); and `Applicable`'s result is `APPLY`/`DEFAULT`/`DO_NOT_APPLY`, not allow/deny.

## The systems

Two systems, each a `@ModSystem` the root builds, with their data and rules written once. Nothing here is mod
content: the effects, attributes, tags and rows these read are written by the feature systems that own them. The
third, `control`, is in "Removed 2026-11-10".

| Piece | What it is |
|---|---|
| **`effect`.** | `module/system/effect`. `Effect`, `@ModSystem(Dictionary.EFFECT)`, takes `attachment` and `log` (the plan row's network and schedule are dropped: the rules need neither, and no payload exists since a client reads a synced effect and a synced tag). It declares the `Owned` key in its constructor and handles `MobEffectEvent` `Applicable` and `Added` and its own `ClearEvent`, registered by the root on the game bus. Public: `hold` and `cure`, below. |
| **Effect data.** | `EffectData`, a `@Value` closed set of data map types, registered by `EffectContent` (`@Content`, no registers, its `register(IEventBus)` adds the `RegisterDataMapTypesEvent` listener, as `module-plan.md` fixes). All server-only. `STACKING` on mob effects: `Stacking(Optional<Integer> maxAmplifier)`, absent for no cap. `EFFECT_PROTECTION` on mob effects: `Protection(HolderSet<MobEffect> blocks, boolean cleanse)`. `ATTRIBUTE_PROTECTION` on attributes: `Protection(blocks)` (a protecting attribute cannot cleanse: nothing tells when it changed). `SCALING` on attributes: `Scaling(Optional<MobEffectCategory> category, Optional<HolderSet<MobEffect>> effects, Direction direction)`, at least one filter (both means both), `Direction` `LENGTHEN` or `SHORTEN`. `MobEffectCategory` is a plain enum with no codec, so `EffectData` defines one: a string codec over `name().toLowerCase(Locale.ROOT)` that refuses unknown names. The tag `EffectTags.INCURABLE` on mob effects. Ids from Dictionary words under the mod's namespace. |
| **Stacking.** | On `Added` with an old instance: `merged = min(old.amp + added.amp + 1, max(cap, old.amp))`, never above 255; duration: infinite if the incoming instance is infinite, the incoming finite duration if only the old one is infinite (so the game's `update` pushes the held infinite instance down the chain, where `hold` still finds it, instead of the merge overwriting it and making the stack permanent), else `max(old, added)`; the incoming instance is edited in place (through the access transformer) and the game's own `update` then raises the old one. No nested `addEffect`, no guard, no second `Applicable`. An effect without an entry is not touched. |
| **Protection.** | On `Applicable`, server side only. Each protector the entity has whose `blocks` holds the incoming effect rolls its strength: an effect protector present is strength 1, an attribute protector is `clamp(value, 0, 1)`, a roll of `random.nextDouble() < strength`; the first success sets `DO_NOT_APPLY` and the rest are not rolled (certain ones first, so the draws are spared). The old distribution holds. On `Added`, an effect with `cleanse` removes every effect its `blocks` holds, through the system's own removal. |
| **Scaling and immunity.** | On `Applicable`, a `SHORTEN` entry whose attribute value is 1 or more on an entity blocks every effect its filter matches (replaces the class check). On `Added`, before stacking, for each `SCALING` attribute the entity has: a finite incoming duration is multiplied by `1 + max(0, v)` for `LENGTHEN` and by `1 - clamp(v, 0, 1)` for `SHORTEN`, rounded, at least 1; the factors multiply; infinite and hidden instances are not scaled. |
| **Cure and incurable.** | The old tree's NeoForge cure API rebuilt, since 26.1.2 dropped it. Milk and the totem were the default cures every effect carried, and an effect with an empty cure set (the curses, Moon Shield) was cured by nothing; now an effect in `INCURABLE` is one the clear leaves. A mixin on `ClearAllStatusEffectsConsumeEffect.apply` (milk's and the totem's clear, and any item's that uses it) posts `ClearEvent(entity, stack)`, an `@Event` in the system; `Effect` answers it by removing every active effect not in `INCURABLE` through `removeEffect`, and the mixin returns that answer in place of vanilla's `removeAllEffects`. `cure(LivingEntity, TagKey<MobEffect>)` is the old `removeEffectsCuredBy`: it removes every active effect the tag lists, as a feature's named cure (the horn's) did. A feature owns its cure tags; the system owns none. `Remove` is never cancelled, so `/effect clear` and every other mod's removal work as they always did. |
| **Owned infinite effects.** | `hold(LivingEntity, Identifier source, Held held, boolean active)` with `Held(effect, amplifier, ambient, visible, icon)`, called by a source each tick or when its condition changes; one `source` per held effect. `Owned(Set<Identifier>)` is a saved attachment (not kept on death, as the effects are not). Active: no instance of the effect on the entity, add an infinite one and, if the game took it, record the source; an instance already there is never replaced, and an owned source whose infinite instance has vanished from the chain is forgotten. Inactive: if the source is recorded, unlink the infinite instance of that amplifier from the chain (the game's `update` gives a hidden chain only to a finite head, so an infinite instance never has anything under it: held as the head it is removed alone, held below a finite head it is spliced out in place, and nothing passes through the rules again), forget the source; if not, return before touching anything (`isSet` is false for nearly every entity). Reads and writes `hiddenEffect` through the access transformer. |
| **`damage`.** | `module/system/damage`. `Damage`, `@ModSystem(Dictionary.DAMAGE)`, takes `registration` and declares the datapack registry `jolcraft:affinity` (not synced: only the server reads it). `Affinity`, a `@Value` record and the registry's entry: `damage` and `except` (`HolderSet<DamageType>`, `except` default empty), `resistance` and `vulnerability` (`Holder<Attribute>`), and `innate`, a list of `Innate(HolderSet<EntityType<?>> entities, double resistance, double vulnerability)`, both 0 or more and the resistance at most 1. One row replaces a damage tag, six entity tags and the constants 1.0 and 0.5: the old rows' immune, resistant and vulnerable tags are three `innate` entries. |
| **Applying a row.** | At `LivingDamageEvent.Pre`, for every row whose `damage` holds the source's type and whose `except` does not: `resistance = clamp(value, 0, 1)`, `vulnerability = max(0, value)` (an entity without the attribute counts 0), `damage *= (1 + vulnerability) * (1 - resistance)`; rows multiply, so a type in two rows takes both; the result is `max(0, damage)`. On `EntityJoinLevelEvent` for a server-side living entity, for each row the greatest `resistance` and the greatest `vulnerability` among the `innate` entries that hold the entity's type become a transient `ADD_VALUE` modifier on the row's two attributes, removed when 0; the modifier ids are the row's id and a word. The write is idempotent: a non-zero value uses `addOrUpdateTransientModifier`, 0 uses `removeModifier(id)`, because a player changing dimension stays the same object and posts the join again, where `addTransientModifier` would throw. |
| **`Stage`.** | A `@Value` set of `EventPriority` constants, the one place the order of damage handlers is written. `LivingIncomingDamageEvent`: `ATTACK` (highest: what the attacker adds), `SHAPE` (high), `TARGET` (normal: what the target's effects do before mitigation), `DEFENSE` (low: flat defences). `LivingDamageEvent.Pre`: `AFFINITY` (high), `FINAL` (normal: the last defences, after affinity). `Post` is read-only and has no stage. A system registers its listener at its stage; `Damage` registers affinity at `AFFINITY`. |

## Lifecycle and rules

Built: the root builds the two systems, calls `EffectContent.register(modBus)` and adds each system's listeners
(`effect::onApplicable`, `onAdded`, `onClear` on the game bus; `damage`'s two); `Damage` has declared the registry to `Registration`, `Effect` the attachment to
`Attachment`, both before the root registers them. Used: the game posts the events; each handler returns at once
unless the entity is on a server level, the data has an entry and the entity has a matching effect or attribute.
Data maps and tags are read live from the registries, so `/reload` takes effect; affinity's innate modifiers are
written at join and follow a reload only when the entity next joins.

- Every rule acts on the server. A client sees synced effects and tags only; the data maps and the affinity registry
  never reach it, and `forceAddEffect` on a client does not fire `Added`.
- `Applicable` and `Added` return at once for a `ServerPlayer` not yet added to its level
  (`!entity.isAddedToLevel()`): that is `restoreFrom` re-adding a player's effects after the End, which would roll
  protections again, rescale durations (x1.5 per trip at 0.5) and cleanse again.
- The instance handed to `addEffect` is edited by `Added`; a caller passes a fresh one, as the game already requires.
  `forceAddEffect` posts no `Added`, so it bypasses stacking, scaling and cleanse, but it posts `Applicable`, so protections hold.
- The order inside `Added` is cleanse, scale, stack. A protection blocks before any of them.
- `Applicable` only ever answers `DO_NOT_APPLY`, never `APPLY`, so another mod's refusal stands.
- Incurable only means the clear leaves it: milk, the totem and any item using that consume effect. A feature's cure,
  `/effect clear`, expiry and death remove it as before.
- A mixin holds no reference to a module: it reads the vanilla object, posts `ClearEvent` and reads its answer.
  `ClearEvent` is in `effect`'s folder; the architecture test's rule 8 (a mixin depends on no module) holds since it is
  not a module.
- Rule 4: `Effect` takes attachment and log, `Damage` registration; neither has a client half.
- The AT is the smallest opening that works: `MobEffectInstance.duration`, `amplifier` and `hiddenEffect` public (in-place
  stacking and scaling, and reading the chain; the 7-argument constructor can rebuild a chain but cannot read one or edit
  the instance `Added` carries, and `forceAddEffect` with a merged copy would leave vanilla's `update` to refresh the
  attributes from the stale old instance). A mixin accessor for them is more code for the same three fields.
- Cost: `Applicable` walks the protection and scaling maps, a handful of entries; `Pre` walks the rows, six or so.

## Proof

Unit tests under the main mod (registers filled): each record's codec round trips and refuses what it must (`Scaling`
with no filter, `Affinity` with a resistance above 1, a negative vulnerability, the category codec refusing an unknown
name); `Stacking.merge` (the old arithmetic: amplifiers add plus one, cap, an old amplifier above a lowered cap, an
infinite incoming stays infinite, a finite incoming on an infinite old stays finite, 255), `Scaling.factor` (both
directions, 1 or more blocks, minimum 1 tick), `Protection.roll` with a seeded `RandomSource`, `Affinity.multiplier`
(two rows multiply, `except` excludes, an absent attribute counts 0, the innate maximum), `Owned` round trip.

Scenes on the game-test server under `gametest/module/system/{effect,damage}`, on vanilla effects, attributes,
damage types and mobs, with the fixtures the scenes read, a built-in data pack in `src/gametest/resources/scene` (its own `pack.mcmeta` and `data/`
tree, registered by `ScenePack` through `AddPackFindersEvent`). The pack is required on `runGameTestServer`, whose build.gradle
run sets the system property `jolcraft.gametest.scene`, and listed but off in `runClient`, `runClient2` and `runServer`: its source does
not add it to a world by itself, so a developer turns it on with `/datapack enable "mod/jolcraft_gametest:scene"`. A data map or tag file sits in
the folder of its type's or key's namespace, so those files are under `scene/data/jolcraft/` (the dev mod ships no
jolcraft-namespaced content of its own, only fixtures for the mod's rules, naming vanilla things; the real mod's data
merges beside them); affinity rows are entries under `scene/data/jolcraft_gametest/jolcraft/affinity/`; the dev mod's cure
tag and its loot modifiers are in the pack too. What stays in the plain mod resources is inert in play: its recipes of its own kinds,
its advancements, blockstates, the mixin config, mods.toml. The dev mod registers no attribute: the fixtures name vanilla living attributes that default to 0
(`explosion_knockback_resistance`, `oxygen_bonus`, `movement_efficiency`, `water_movement_efficiency`, and for the damage fixtures `attack_knockback`, `knockback_resistance`, `armor`, `max_absorption`
and `luck` (player-only, so a mob lacks it); not `burning_time`, which defaults to 1) and the
scenes set base values on their own mobs. The fixtures load in every scene and, in a dev client, only when the pack is enabled by hand, so the attribute
fixtures stay inert at default values, and `innate` entries name a mob type no other scene uses. A tag fixture acts
whenever its effect is present, so the effects the fixtures name are ones the game-test server meets only by command (`luck`,
`unluck`; `slow_falling` only in rules no other scene reads). No asset, no English, no mod content.

- **effect.** On a cow: stacking at a cap of 2 goes to amplifier 0, 1, 2 and stays 2, with the longer duration, and an
  infinite application keeps the instance infinite; a finite stack on a held infinite instance stays finite, expires
  and the held instance comes back, and it is gone once the source stops; the attribute refreshes to the merged amplifier (the vanilla
  `update` path). An attribute protector at 1 blocks and at 0 does not; an effect protector blocks and, with `cleanse`,
  removes the blocked effect already there; `blocks` not holding the effect lets it through. A `LENGTHEN` attribute at
  0.5 gives 1.5 times the duration, a `SHORTEN` at 0.5 half, at 1 the effect is not applied; scaling happens before
  stacking. a milk bucket drunk by a survival `TestPlayer` and a totem's death protection leave an incurable effect and
  take the others; `/effect clear` takes the incurable one too; `cure` removes what its tag lists and nothing else. `hold`: adds an infinite instance and records the source; a second `hold` changes
  nothing; a stronger finite effect pushes it down the chain, and when the source goes inactive the chain no longer
  holds the infinite instance after the finite one expires; an infinite instance the source did not add is not removed;
  an inactive call on a fresh entity leaves no attachment (`isSet` false); with a held infinite instance under a finite
  head, a `SHORTEN` attribute and a 0.5 attribute protector on the entity, stopping the source leaves the head
  present with its duration unchanged. If `TestPlayer` can reach `restoreFrom`, a scene re-adds a player's effects
  and asserts no protection roll and no rescale; else it is on the hand check.
- **damage.** On a cow hurt through `hurtServer` with a fire and a magic source: a fire row with resistance 0.5 and
  vulnerability 0.25 gives `(1.25)(0.5)` of the damage; a type in two rows takes both; `except` excludes; an entity
  type in an `innate` entry takes the modifier on join and none on another type; a row with no attribute on the entity
  changes nothing; posting the join twice for an innate type does not throw and leaves one modifier; a listener at `Stage.FINAL` sees the damage after affinity and one at `Stage.AFFINITY` before it.
  These are the first scenes to read a datapack registry in a running game, which settles W1 for `Registration.datapack`.

## Where things go, and the order

Four steps, each ending with the gates in `CLAUDE.md`.

1. **Rules.** `src/main/resources/META-INF/accesstransformer.cfg` with the three `MobEffectInstance` fields;
   `Effect`, `EffectContent`, `EffectData` and its records, `EffectTags`; stacking, protection, scaling and immunity;
   the Dictionary words (affinity, amplifier, attack, blocks, category, cleanse, control, direction, effects, except,
   incurable, innate, lengthen, look, move, protection, ranged, resistance, scaling, shorten, stacking, stops, target,
   use, vulnerability and the rest the ids need); root wiring; unit tests and the effect scenes' rules half.
2. **Cure and hold.** `Owned`, `ClearEvent` and its handler, `cure`, `hold`; `jolcraft.mixins.json`, the
   `[[mixins]]` entry in the mods.toml template and the clear mixin, the first mixin in main; the rest of the effect
   scenes.
3. **Control** (built and removed 2026-11-10, see "Removed 2026-11-10"). `Control`, `Stop`, `GoalEvent`, the server handlers; the four goal mixins in the same config; the AT's
   `moveVector` and inner-class lines;
   `ControlClient`; the control scenes and the hand check. The spell goal's mixin reaches its mob through the synthetic
   outer reference (`this$0`), which the old tree did; if the synthetic field cannot be shadowed in 26.1, the fallback is a mixin on
   `SpellcasterIllager` itself, and the evoker scene is what shows it.
4. **Damage.** `Damage`, `Affinity`, `Stage`, the two listeners, the datapack registry; the damage scenes.

No datagen change in these steps: the contributors for mob effect, damage type and entity type tags, the data map
and the affinity rows come with the first feature that has data to write, as the seams were added so far, and the
scenes use hand-written JSON. Docs after the build: the `effect` row of `module-plan.md` becomes three rows (`effect`,
`control`, `damage`, with their dependencies as above) and its Status; `module-map.md` names the mixin config and the
access transformer; `CLAUDE.md` gains the lines for the new folders, the AT and the mixin config, mirrored into
`AGENTS.md`, and says `src/main/resources` now holds those two files beside the hand-made assets.

## Not in this step

- Armour penetration, the projectile bonus, the heal increase, Overheal, Moon Shield, Sunfire, Luminance critical, the
  Slowness rescale and the curse family: each is one attribute's behaviour on a NeoForge event and goes with the
  system that declares the attribute, at its `Stage` (armour penetration is an `ARMOR` reduction modifier added at
  `ATTACK`, 26.1.2 has `addReductionModifier` as before).
- Fire immunity by tag and the two wraps that stop vanilla's Fire Resistance and the freeze multiplier: they exist so
  the mod's attributes govern what vanilla decides outright, so they come with the first feature that owns such an
  attribute. The seams are `Entity.fireImmune` (read at five sites in `Entity`, and `EntityType.isBlockDangerous` reads the field)
  and the `hasEffect` call at `hurtServer` 1201 and the `is(FREEZE_HURTS_EXTRA_TYPES)` call at 1224; the old mixins
  become `@Desc` mixins that post an event.
- Mod effects that act through attribute modifiers (Rooted's speed and jump) are `MobEffect.addAttributeModifier`,
  the game's own; the old `MobEffectsMixin` is feature content.
- The datagen seams named above, and a language or `Completeness` demand for effect and tag names: with the first
  data.

## Challenged 2026-11-10

All eight findings that reached the check were confirmed (two in part, and two pairs of reports were merged into
one each). Changed: the stacking duration no longer lets a finite stack make a held infinite instance permanent; an
End-portal return (`restoreFrom`) no longer re-rolls protections or rescales durations; `hold`'s unlink runs under the
system's one operation field and no longer re-protects, rescales or cleanses the remainder; the innate modifier write is
idempotent for dimension changes; the dev mod registers no attribute and its fixtures use vanilla ones that default to
0; the crossbow and melee `stop()` effects are described as the sources show; `MobEffectCategory` gets a codec in
`EffectData`; the wololo goal is named as outside `USE`. Refuted: `luck` as a spare attribute (player-only),
`forceAddEffect` as a way around `Applicable`, the crossbow losing its loaded bolt, and its charging flag being left set.

## Settled without the author

The four goal mixins are built: the finished codebase disarms mobs, and nothing but a mixin stops a skeleton drawing
or an evoker casting. A blocked goal ends through vanilla's own `stop()` and other goals take over, so a disarmed
archer wanders instead of strafing and a disarmed melee mob stops pursuing but keeps its target. Armour penetration, the
projectile bonus and the fire and freeze overrides stay with the features that declare their attributes, since each is
one attribute's behaviour (see "Not in this step"). The goal mixins were reversed by the author the same day, see
"Removed 2026-11-10".

## Decided 2026-11-10

The old cure API is rebuilt over a mixin, since NeoForge 26.1.2 dropped it: the clear mixin posts `ClearEvent`,
`INCURABLE` stands for the empty cure set and `cure` for `removeEffectsCuredBy`. `Remove` is never cancelled. The old
tree never cured an effect by milk and not by the totem, so one tag serves both.

## Built 2026-11-10 (step 1)

Built: `accesstransformer.cfg` with the three `MobEffectInstance` fields; `Effect`, `EffectContent`, `EffectData` with
`Stacking`, `Protection`, `Scaling`, `Direction` and the category codec; the Dictionary words; root wiring; unit tests
(`EffectDataTest`); eleven scenes in `EffectScenes` over four fixture data maps under `scene/data/jolcraft/data_maps` (the scene pack). Departures:

- `Effect` takes `Log` only. The attachment comes with step 2, where `Owned` is declared. `EffectTags`, `Owned`, `hold`,
  `cure`, `ClearEvent`, the `operation` field and every mixin are step 2 or later.
- `Effect.register(gameBus)` adds `onApplicable` and `onAdded`; `onClear` comes with step 2.
- An effect protector that cleanses does not remove itself when its own `blocks` lists it: the merge path would otherwise
  be handed an old instance the cleanse already took out of the map.
- `EffectData.CATEGORY` is declared before the data map types, since the nested `Scaling.CODEC` reads it while they are built.
- The attribute protection codec reads and writes only `blocks`; a `cleanse` key in its data is ignored.
- The End-portal scene builds a `ServerPlayer` the way the game does and calls `restoreFrom(old, true)` on it before it is
  added to the level, so the guard is proven directly.
- The `merge` of two infinite instances, and of a finite incoming on an infinite old one, follow the table; the fixture
  luck stacks to 2 and unluck stacks without a cap, so the game-test server meets them only by command, and a dev client not at all unless the scene pack is enabled.

## Built 2026-11-10 (step 2)

Built: `Owned` (a `@Value` record over `Set<Identifier>`, its `MapCodec` a `sources` list), `Effect.OWNED`, `Effect.Held`,
`hold`, `cure`, `onClear`; `ClearEvent`, `EffectTags.INCURABLE`; `ClearAllStatusEffectsConsumeEffectMixin`, `jolcraft.mixins.json`
and its `[[mixins]]` entry; `Effect` takes `Attachment` and `Log`. The totem's death protection runs this consume effect
(`DeathProtection.applyEffects` calls `ConsumeEffect.apply` on each effect, `TOTEM_OF_UNDYING` starting with the clear), as
does milk (`Consumables.MILK_BUCKET`); `/effect clear` calls `removeAllEffects` directly and is untouched, as designed.
Departures:

- No operation field. The design's re-add of a remainder under a held head serves a chain the game never builds
  (`update` gives a hidden chain only to a finite head, and an infinite instance is never the shorter), so the held
  head is removed alone and a held node below a finite head spliced out; nothing re-enters `Applicable` or `Added`. The
  first build kept the re-add behind a mutable static field, there only so the game-test mod's own `Effect` instance
  could reach the real one's listeners; it was removed.
- `OWNED` is a public static constant of `Effect` so a scene can ask `isSet`.
- `hold`'s inactive path forgets the source with `reset` when it was the last one, so an entity that held nothing again
  carries no attachment; `forget` also runs when an active call finds a recorded source whose infinite instance has left
  the chain.
- `cure` answers whether it removed anything; `onClear` answers true when the entity had an effect outside `INCURABLE`
  (vanilla's `removeAllEffects` answers true for any active effect, so an entity with only incurable effects answers
  false here: nothing was cleared).
- The SHORTEN-and-protector case runs on the chain the game builds, a finite head over the held instance, where the
  unlink splices; the held head's removal is the stacking scene's last check.
- `unlink` answers false when another mod cancels the held head's `Remove`, so the source stays recorded and the next
  call tries again.
- Fixtures: `scene/data/jolcraft/tags/mob_effect/incurable.json` lists unluck; `scene/data/jolcraft_gametest/tags/mob_effect/cure.json`
  lists speed and unluck; the attribute protection fixture also lists hero of the village under explosion knockback
  resistance, so the 0.5 protector has an effect to refuse.
- `JolCraftGameTests.effect()` joins the dev mod's static accessors, the exception to rule 5 `module-map.md` states.

## Built 2026-11-10 (step 3)

Built as designed, with `StopTest` and twelve scenes, and removed the same day (see "Removed 2026-11-10"). Its build
notes, the departures from this design that the code and the scenes found, are in this file at `dd43d50d`.

## Built 2026-11-10 (step 4)

Built: `Damage`, `Affinity` (with `Innate`, `Strength`, and the pure `applies`, `factor`, `multiplier`, `innate` and the two modifier ids),
`Stage`; the Dictionary words (affinity, entities, except, innate, resistance, vulnerability); root wiring (`Damage` built
with `registration`, `register` on the game bus); `AffinityTest` and seven scenes in `DamageScenes`. The registry is
empty in the main mod; the old six rows and their attributes are feature content. Departures:

- Fixtures are three rows under `scene/data/jolcraft_gametest/jolcraft/affinity/`, naming vanilla things only. `fire`: `#minecraft:is_fire`
  except lava, resistance `attack_knockback`, vulnerability `max_absorption`. `warmth`: `on_fire` and `magic`, resistance
  `explosion_knockback_resistance`, vulnerability `luck` (a player attribute, so a cow lacks it, which is the absent-attribute
  case). `innate`: `generic_kill` only (a command's damage, so the row is inert in play), resistance `knockback_resistance`,
  vulnerability `armor`, two innate entries for rabbits (0.2 and 0.5, then 0.6 and 0.1, so the greatest of each comes from a
  different entry). Only the effect scenes set explosion_knockback_resistance, on cows they never hurt with fire or magic; no other scene uses the rest or rabbits. They are inert at default values. What changes in a dev client only once the scene pack is enabled by hand: max absorption under
  Absorption, Blast Protection armour, the mobs whose base attack knockback is above 0, and the Luck effect on a player; the
  rabbit's two innate modifiers are a trivial knockback resistance and armor. (armor_toughness and mining_efficiency were first
  chosen and swapped after review: diamond armour and Efficiency tools raised them in ordinary play.)
- The stage scene proves the order with a listener at `EventPriority.HIGHEST`, ahead of `Stage.AFFINITY`, which sees the damage as
  it came, and one at `Stage.FINAL`, which sees it after the row. A listener registered at `Stage.AFFINITY` after the mod's own would
  run after it (the same priority runs in registration order), so it cannot show "before".
- `Damage` applies `Affinity.multiplier` and writes the modifiers through `Affinity.resistanceModifier` and `vulnerabilityModifier`
  (`<row path>_innate_resistance` and `_vulnerability` in the row's namespace).
- The unit test decodes rows under the vanilla datapack lookup but does not round trip them: the lookup's holders are not valid for
  encoding in a second lookup, and nothing encodes affinity rows until datagen writes them.
- The damage scenes are the first to read a datapack registry in a running game; see W1 in `REVIEW_TODO.md`.

## Removed 2026-11-10

The author: mixins on vanilla classes made for one planned feature (a stun) are specific work, not the generic layer,
while support for changing how damage works is fine; the clear mixin stays, generic enough and the replacement of the
cure API NeoForge 26.1.2 dropped. Crowd control is the work of the features that own stun, root, suppress and disarm, so
`control` went: `Control`, `Stop`, `GoalEvent` and `client/ControlClient`, the four goal mixins, the access
transformer's `ClientInput.moveVector` and spell-goal lines, the root's wiring, the seven Dictionary words only it used,
`StopTest`, the twelve scenes in `ControlPlayerScenes` and `ControlMobScenes`, and the six stop-tag fixtures in the scene
pack. Its client half was never checked by hand. The code is in history at `dd43d50d`; the research above (what the old
tree did, what 26.1.2 gives, the seams and the goal targets) is where that feature starts. Verified after the removal:
205 unit tests, `runData`, 113 of 113 scenes and `build` green.
