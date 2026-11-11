# Outcome: the mod's yields, and their delivery

Draft of 2026-11-11. The author decided that the mod's one `YieldKinds` lives in a system of its own, and that this
system also delivers what a result rolls (`hand.md`, "Decided"). Built on 2026-11-11 as step 2 of `hand.md`, before
its first host, see "Built 2026-11-11". Not challenged.

## Why a system

`Result` rolls yields and answers a `Delivery`, and on purpose acts on nothing. Giving goes through `Inventory`, and a
composite may not use another (rule 3), so only a system can turn a delivery into the world. Without one, every host
does it again: the old tree re-dispatched on the concrete class in each host, a fault `result.md` names. The mod's one
`YieldKinds` also needs a home: a record embeds it before any module exists, so it is a `@Keys` constant, and a
`@Keys` set sits in a system folder.

## The system

| Piece | What it is |
|---|---|
| **`outcome`.** | `module/system/outcome`. `Outcome`, `@ModSystem(Dictionary.OUTCOME)`, takes `inventory` and `log`. The root builds it before the hosts and hands it to them through their constructors. |
| **`OutcomeKeys`.** | `@Keys`: `YIELDS = YieldKinds.of(Ids.of(Dictionary.JOLCRAFT, Dictionary.YIELD))`, the same two registries the root declares today, `jolcraft:yield` and `jolcraft:yield_hook`. The root declares it to `Result` from here, and every host's records embed it. It reads nothing of a host, so a host's keys, then its record, then `OutcomeKeys` initialise one way. |
| **Delivering to a player.** | `deliver(Delivery, ServerPlayer)`, on the server thread. The stacks are given together through `inventory.give(Holder.Player)`, in the game's place-back order, so they stack with each other and the overflow drops once, at the player, as `/give` drops it. Each entity of each count is made by the game's spawning `create` at the player's block, finalised as a mob is, with `EntitySpawnReason.EVENT`. It is moved to the player's own position with a random yaw and added, as the old tree did; one the game refuses is skipped. Each effect is added to the player as a fresh copy through the game's `addEffect`, so the effect system's rules apply and the delivery the caller holds is not edited. Each sound plays at the player for everyone in range, as the old tree played it. A value of another kind is the caller's, read through `delivery.of(kind)`, and is left alone. A refused or empty delivery does nothing. It answers nothing, and logs at debug what it gave, spawned, added and played, with any overflow dropped. |

What waits for its first consumer:
- A recipient that is a place or a block, such as a structure's trap or a dwarf's drop. That consumer also settles
  what an effect does with no entity to land on.
- Mod-wide kinds and hooks no single feature owns, such as a particle yield. They are declared here through `Result`
  when they come, and `Outcome` then takes `result` as well.

## Lifecycle and rules

Built: the root builds `Outcome` with `inventory` and `log`, declares `OutcomeKeys.YIELDS` to `Result` in place of
today's inline `YieldKinds.of(...)`, and hands `outcome` to the hosts. The registries keep their ids, so no data
changes. The game-test mod keeps its own `Result` and yields under its own namespace.

- No content, no actor, no mixin, no client half, nothing registered: `deliver` is a method and the keys are a
  constant.
- Rule 4: `Outcome` takes `inventory` and `log`. `OutcomeKeys` depends on a handle, `Ids` and the Dictionary.
- A host keeps what is its own: when to deliver, what a refusal means, its sounds of success and failure, and any
  kind of its own.

## Proof

Unit test: `OutcomeKeys.YIELDS` names `jolcraft:yield` and `jolcraft:yield_hook`. Scenes under
`gametest/module/system/outcome`, isolated, with a survival `TestPlayer` and deliveries built from `Yielded` values,
so no data is needed. The game-test mod builds an `Outcome` of its own over its own `Inventory`, as it builds its
`Effect`; `Outcome` declares nothing, so a second instance is harmless. The checks:
- Stacks are given into the inventory, and the overflow of a full one drops targeted at the player.
- An entity count is spawned at the player.
- The effect reaches the player, and the instance the player holds is not the delivery's.
- A sound reaches the player.
- A refused delivery and a value of another kind change nothing.

## Where things go

Step 2 of `hand.md`. That is `Outcome`, `OutcomeKeys` and the Dictionary word outcome, plus root wiring: the
`YieldKinds` line moves and `Outcome` is built. Then the unit test and the scenes. Docs:
- the `outcome` row of `module-plan.md`'s shared systems;
- the `CLAUDE.md` line, mirrored into `AGENTS.md`;
- `result.md`'s note on where the root takes the `YieldKinds` from.

## Settled without the author

- Only a player is a recipient for now.
- The stacks go in one give.
- Effects are copied.
- Sounds are heard by everyone in range.
- Entities are spawned as the old hand interaction spawned them.

## Built 2026-11-11

Built: `Outcome`, `OutcomeKeys` and the Dictionary word outcome; the root declares `OutcomeKeys.YIELDS` to `Result`
in place of its inline `YieldKinds.of(...)` and builds `Outcome` first among the systems, unused until `hand`; the
dev mod builds one over its own `Inventory`. `OutcomeKeysTest` checks the three ids, and four scenes in
`gametest/module/system/outcome`: two stone stacks stacking in one slot, then a full inventory's overflow tossed once
and targeted at the player; two chickens and an armor stand at the player's exact position; haste added in an
instance of the player's own and a bell heard at its pitch; a refused delivery carrying all four kinds and a value
of another kind changing nothing. Verified: 206 unit tests, `runData`, 119 of 119 scenes and `build` green.

Where the build goes past the plan:
- A spawned entity is added with its riders, as the game's own spawn adds them; the old tree's add left a jockey's
  rider unadded.
- The yaw is the one the game's `create` rolled and gave a mob's head and body, kept when the entity is moved to the
  player, rather than a second roll.
- `deliver` refuses to run off the server thread, as `Inventory` does.
