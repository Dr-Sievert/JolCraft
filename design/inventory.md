# Inventory: items counted, taken and given against a holder's handler

Draft of 2026-10-27 from a scout of NeoForge 26.1.2.122's transfer API, vanilla 26.1.2's inventory side, the old
tree's 24 inventory operations and our own seams, challenged by three reviewers (rules, fidelity to the old hosts,
26.1 feasibility; 7 findings of 25 confirmed and folded in); built 2026-10-27, both steps. The `inventory` composite of `module-plan.md`.

## What the old tree did

Two helpers, 400 lines (`world/item/inventory`), plus `ItemInputAction`: give a stack to a player by merging into
indices 0 to 35 then empty slots then `player.drop`, give into a bench slot then the player, consume or damage the
two held stacks after a recipe matched, consume from a hand with a crafting remainder swapped in, take up to N of an
exact item for a crate, scan the whole inventory for a match, `Inventory.add` then drop. Every host was a vanilla
`Inventory` or `Container` addressed directly; no item handler, no capability, four of seven helper methods unused,
three different creative checks, five different slot ranges, and a merge that replaced the slot's stack left a host
applying a fate to a detached one. Coins, bounties, dwarf refunds, menus and station slots are feature logic and stay out.

## What 26.1 gives

`ResourceHandler<ItemResource>`: indices, `getResource`, `getAmountAsInt`, `insert` and `extract` returning the amount
moved, every change inside a `Transaction` that commits or rolls back on close, thread-bound, one root per thread, a
"simulate" being a nested transaction left uncommitted. `ItemResource` is the immutable item-plus-components key;
a stack's count is the amount. `Capabilities.Item.BLOCK` (sided), `ENTITY`, `ENTITY_AUTOMATION` (what hoppers ask an
entity for) and `ITEM` hand out handlers; NeoForge registers them for vanilla containers, for the player
(`PlayerInventoryWrapper`: 0 to 35 main, 36 to 39 armor, 40 offhand; its `insert` fills a hand already holding the
resource, then stacks into the main slots, never an empty offhand or armor) and, at low priority, for every living
entity's hands and armor; a mod's own container block entity gets nothing until the mod registers a provider.
`ResourceHandlerUtil.insertStacking`, `extractFirst`, `move`; no count helper. The legacy `IItemHandler` packages are
deprecated for removal. `ItemPredicate` tests `ItemInstance`. A container wrapper restores the slot's original stack
object in place at commit, so a captured stack is updated, not detached.

## The composite

`Inventory`, `@Composite(Dictionary.INVENTORY)` in `composite/capability`, over `capability` and `log`; its class
shares the game's simple name as `Recipe` and `Stat` do. Server only: a holder resolves a `ServerLevel`, and one built
on a client level is refused with the remedy. Every public operation that changes something opens its own root
transaction and commits or rolls back before it answers, so a caller never sees a transaction and the change is all
or nothing over the inventory's contents, while counting and matching are pure; inside, the holder's own transaction-taking insert is the shared step, since
the game refuses a root inside a root, and a module's container must not call the composite from its own `setItem`,
`setChanged` or `onTransfer`, nor may anything a fate's wear fires (the durability criterion, an advancement's reward). A player and a hand reach their handler through the game's
player inventory wrapper, an entity and a block through the capability lookups.

| Piece | What it is |
|---|---|
| **Holder.** A nested sealed interface, a value a system builds per call: `Player(ServerPlayer)`, `Hand(ServerPlayer, InteractionHand)`, `Entity(Entity)` (refusing a player, which is held as `Player`), `Block(ServerLevel, BlockPos, @Nullable Direction)`. It resolves its handler when an operation runs, through the game's lookup of the kind (`entity.getCapability(Capabilities.Item.ENTITY)`, `level.getCapability(Capabilities.Item.BLOCK, pos, side)`), so a holder stays valid while a chest is broken or a capability invalidated; a holder without a handler counts nothing, matches nothing and takes every given stack as overflow. A player's handler for counting and taking is main plus offhand, never the worn armor, which the default slotless `extract` would reach; a hand is that one slot; a living entity without a provider of its own is its hands and armor, so a give to it equips. A block holder's side bounds which slots are read and what may be put in through that face, while a sided container may still refuse to give up what it shows, so a count or a match can include what a pay then answers false for. Overflow is dropped at the holder after the commit: a player's through `player.drop(stack, false)` (the toss event, from eye height along the look, no thrower) and then, as `/give` does, no pickup delay and targeted at that player, so only they pick it up and the moment there is room, an entity's through `spawnAtLocation`, a block's as an item entity at the block's centre with the default delay of 10; split by max stack size; a drop the toss event cancels is logged at warn, since the game has already removed the items. The player and hand holders carry the `ServerPlayer` as the **payer** for creative; the others pay in full. |
| **Counting.** `count(holder, predicate)` sums the amounts of every index whose resource matches a `Predicate<ItemStack>` tested on the resource's one-item stack (an `ItemPredicate` passes as `predicate::test`); `has(holder, predicate, amount)`. Pure, no transaction. |
| **Matching.** `match(holder, inputs)` with a list of the recipe primitive's `ItemInput` values answers `Optional<Match>`: for each input, in order, the first resource of the holder (index order) that the input's predicate accepts and whose total across the holder, less what earlier inputs of this match already claimed, covers the fate (`ItemInput.matches` on `resource.toStack(available)`, so a consume fate needs the amount's maximum and a damage fate a damageable item); one resource per input, never two kinds mixed, a consume claiming its largest amount and a damage or a keep the one item it needs present, one pass in input order with no backtracking, so a recipe whose inputs overlap (a tag and one of its items) lists the narrower input first; refused whole if any input finds none. A `Match` holds its holder and, per input, the resource and the matched stack with the available count, the stacks a host puts into its loot context so hooks and conditions read what was matched. Pure. |
| **Paying.** `pay(match)` applies the fates in one transaction, in this order: every consume is rolled through `Fate.apply` on a scratch stack of its resource, the one place the roll and the creative rule live, so it answers the amount to extract and a creative payer answers nothing, while a damage is rolled once, when its copy is worn; every consume is extracted across the holder's indices and every damage extracts its one item, each extract's return compared with the amount and a shortfall leaving without commit, nothing changed, answered as false; only then each damage's copy is worn through `Fate.apply` (the game's own wear, unbreaking, the break sound and criterion, which no rollback undoes, hence the order) and put back at its index when it survived, elsewhere in the holder when that index refuses it, else as overflow; a consumed item's crafting remainder (a bucket's empty bucket) is given back to the holder, one per item consumed, so a creative payer gets none; then commit and the overflow drops. A keep does nothing. `take(holder, inputs)` is `match` then `pay` for hosts without hooks between. |
| **Giving.** `give(holder, stacks)` inserts each stack stacking into the holder, the given stacks never mutated, empty ones skipped, and answers the stacks dropped as overflow (none when all fit), logged at debug. A player's and a hand's give go through `PlayerInventoryWrapper.of(player).insert`, the hand's own slot offered first so a remainder or a single swapped item lands where the hand was; every other holder's through `ResourceHandlerUtil.insertStacking`, since the interface's slotless insert does not prefer filled slots. A system maps a `Delivery.stacks()` or a `Loot.roll` into it; the composite never sees `Delivery` (rule 3). |
| **Exposing.** `exposeBlockEntity(typeSupplier)` for a module's block entity that is a `Container` (a `WorldlyContainer` with the side, whose `canPlaceItemThroughFace` must accept a null side) declares the `Capabilities.Item.BLOCK` provider through the `capability` primitive while the module is built, with NeoForge's own wrapper, so the composite, hoppers and other mods reach a module's holders the same way; the root registers once. This is the composite's use of its primitive. `exposeEntity` (`ENTITY` and `ENTITY_AUTOMATION` both, as NeoForge does for a chest boat) and `exposeItem` (a `CONTAINER` component) come with their first consumer, as does a held item's own handler (a bundle, a pouch) through `ItemAccess`, a fourth holder kind. |

What stays with the features: a block entity writing its own output slot (`setItem`, since the wrapper honours
`canPlaceItem`, which the old benches refused for that slot) and paying from its own role-named slots, menus
returning slots on close, moving between two holders (`ResourceHandlerUtil.moveStacking` is the game's own), thrown
items at a player, coins and pouches, bounty fills, refunds.

## Lifecycle and rules

Built: a system declares exposures in its constructor. Use, server thread only: a host matches (pure), builds its
context from the match's stacks, asks `result.apply`, pays only when nothing refused, then gives the delivery; a host
without hooks calls `take`; a two-hand recipe is two hand holders matched in either orientation. Creative: matching
still needs the items in hand, as `ItemInput.matches` does; paying costs a creative payer nothing; giving fills a
creative inventory and drops the rest, as vanilla's place-back does. Sync: the player's menu broadcasts on the next
tick and the wrapper broadcasts at commit; nothing to do. The old order flaw: pay before give, and a host never keeps
a captured stack across a pay or a give, since the slot's stack is updated in place at commit and a fate applied to
it twice is paid twice; it reads back through the holder. No sound, text or statistic beyond what `Fate.apply`
plays: those are the host's, through `feedback` and `stat`.

## Proof

Unit tests: the composite's shape and guards, a holder's kinds and refusals; nothing that needs a stack, since the
test JVM cannot make one. Scenes, isolated where they add a player, who is switched to survival by a one-line
`TestPlayer` helper since a fresh one is creative: a give that fills the held stack then the main slots and drops the rest at a full
inventory, the drop targeted at the player with no delay and no thrower, the slot packet reaching the player, an empty stack skipped; a count
across three slots and the offhand and not the worn armor; a match of two inputs over one resource refusing three
plus two of four and accepting three plus two of five; a pay consuming across two slots, rolling a range, a damage
wearing the tool in place and a worn-out tool breaking audibly, a shortfall between match and pay answering false
with nothing changed, a creative payer paying nothing and getting no remainder, a bucket's remainder coming back to
the hand; a hand holder paying from that slot alone; a chest holder given, counted and paid; a block without a
handler dropping every given stack at its centre; a living entity holder equipping a give; the game-test mod's own
three-slot container block entity exposed and reached through `Capabilities.Item.BLOCK` and by the composite.

## Where things go, and the order

`composite/capability/Inventory` with its nested `Holder` and `Match`; `Dictionary.INVENTORY`; both roots build it.
Two steps, one at a time: holders, counting, giving and exposing a block entity with their tests and scenes; then
matching, paying and `take` with theirs. Nothing in `src/main/resources`, no content, no datagen.

## Choices made here, say if one is wrong

A consume is paid by one resource summed across the holder's slots, never by two kinds under one tag; matching is
one pass with no backtracking; a player's taking never reaches worn armor; the composite owns the transaction and
takes no `TransactionContext`; overflow is dropped after the commit, at the holder; the crafting remainder comes
back on every consume; exposing starts with block entities alone; the class is named `Inventory` beside the game's.

## Decided 2026-10-27

Overflow at a player drops as `/give` does: the same toss, then no pickup delay and the entity targeted at the
receiving player, so a reward that did not fit belongs to them, cannot be taken by another player and comes back by
itself when there is room. The old helper's drop, delay 40 and anyone's, was vanilla's default, never a choice.
