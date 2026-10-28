# Station: the container block entity, block, menu, slot and screen bases

Draft of 2026-10-28 from a scout of the old tree's four station menus, three station block entities and their
screens, the two base interfaces they shared, and vanilla 26.1.2's container, menu, hopper and GUI code with
NeoForge 26.1.2.122's patches; challenged by three reviewers (rules, fidelity to the old hosts, 26.1
feasibility; 15 findings of 34 confirmed and folded in, see the end). The shared gui module that
`module-plan.md` left open, decided here as the `station` composite over `menu`. Step 1 (server side) built 2026-10-28.

## What the old tree did

A 177-line menu base (`JolCraftMenu`) that knew only geometry: a panel measured in 16-pixel tiles, at least 11
wide and 3 high, and the vanilla player inventory appended once below it, its slot ranges recorded for the
subclasses. Everything else was repeated per station: `quickMoveStack` four times (283 lines, two tails, a
pestle-only route in one bench and a fall-through route in the other), `stillValid` four times, the client
constructor over `SimpleContainer` plus `SimpleContainerData` plus `ContainerLevelAccess.NULL` twice, the
`clickMenuButton` gate and re-resolution of the block entity twice, and the same slot rule written three times
each (block entity `canPlaceItem`, menu slot `mayPlace`, a tag test inside `quickMoveStack`). A `JolCraftSlot`
with four fluent hooks of which two were ever used (`mayPlaceRule`, `onSlotChanged`), a `JolCraftResultSlot`
that counted takes and fired `onCraftedBy` and NeoForge's crafting event with the bench as the container.
Lapidary and mortar extended vanilla's `BaseContainerBlockEntity` directly and copied 116 lines between them
(container glue, item persistence, name, menu creation, tool wear, output delivery); the strongbox extended
`RandomizableContainerBlockEntity`; two interfaces, `SyncingBlockEntity` (five sync helpers, one used, the
flags inert) and `TickingBlockEntity` (three ticker factories, one unused), were implemented by the brewing
block entities, the hearth and the strongbox, never by the benches. The blocks copied `onRemove`, which
dropped the contents, and a `getRenderShape` override, both obsolete in 26.1. A 705-line screen base drew a
three-layer panel (nine-slice base, tinted block texture with trimmed corners, nine-slice outline) by tile
loops over `RenderSystem` state, slot wells, a dimmed ghost item in an empty tool slot, a hand-rolled item
button hit-tested by tile, and four theme hooks nobody overrode; each screen re-listed the menu's slots to draw
their wells and repeated the button gate and the click send. Two mechanisms refreshed a bench's cached recipe
state (slot hooks in one, `setChanged` in the other), one shared the answer between viewers although the unlock
test is per player. Hoppers could pull from every slot of every bench, inputs and tools included. The
container stations: lapidary bench, mortar, strongbox and its lock front. The hearth, fermenting barrel and
cauldron are plain block entities with no items and no menu; they shared only the tick and sync interfaces. The
dwarf merchant menu extends nothing of this and is not a station.

## What 26.1 gives

`RandomizableContainerBlockEntity`, the base of every vanilla storage container (barrel, chest, shulker box,
dispenser, hopper), over `BaseContainerBlockEntity`: a `Container`, a `MenuProvider` and `Nameable` with a lock
code, a custom name, the `CUSTOM_NAME`, `LOCK`, `CONTAINER` and `CONTAINER_LOOT` item components carried on
place and pick, `setItem(slot, stack, insideTransaction)` for NeoForge's wrappers, `createMenu(id, inventory,
player)` answering null and the locked notification when the lock refuses, and a pending loot table unpacked
on the first access of any accessor or opener (a block entity that never sets one pays two null fields); it
leaves `getItems`, `setItems`, `getContainerSize`, `getDefaultName` and `createMenu(id, inventory)` to the
subclass and saves no items itself (`trySaveLootTable` else `saveAllItems`, as the barrel does). Persistence is
`ValueInput` and `ValueOutput` with `ContainerHelper.saveAllItems` and `loadAllItems`, the latter not clearing
its destination. `BlockEntity.preRemoveSideEffects` drops a `Container`'s contents when the block changes, on
the server, so a block drops nothing itself (the shulker box overrides it to nothing, since its item carries the
contents); `BlockBehaviour.onRemove` is gone and `affectNeighborsAfterRemoval` is for the comparator update.
`getUpdatePacket` and `getUpdateTag` default to nothing, so a block entity sends nothing to clients unless it
says so, and what it sends goes to every client tracking the chunk; `setChanged` marks the chunk and updates
comparators only, and NeoForge's container wrapper calls it once at the commit of a transfer. `BlockEntityType`
is built by constructor, `BaseEntityBlock` still exists with `createTickerHelper`, `getMenuProvider` and no
render shape. `AbstractContainerMenu.addStandardInventorySlots(container, 8, top)` places the 27 main slots and
the hotbar at `top + 58` exactly where the old base put them; `moveItemStackTo` merges without asking `mayPlace`
and fills one empty slot per call; `quickMoveStack` must answer `EMPTY` when nothing moved or the game loops;
`stillValid` is polled twice a tick on the server and before every click; `clickMenuButton` carries one int.
Data slots travel as full ints to a NeoForge client and as shorts to a vanilla one. Hoppers use a block entity
that is a `Container` directly, every slot on every face unless it is a `WorldlyContainer`, and consult
`Capabilities.Item.BLOCK` only when no `Container` is there; NeoForge registers no handler for a mod's container
block entity, which is what `Inventory.exposeBlockEntity` is for, and its wrapper asks
`canPlaceItemThroughFace` with a null side and skips `canTakeItemThroughFace` for one. `Slot.setBackground`
is the empty-slot icon, common code. On the client `GuiGraphics` is `GuiGraphicsExtractor`: `blit` and
`blitSprite` take a `RenderPipeline` and an ARGB tint, items take no tint, text with alpha zero draws nothing,
`renderBg` is an `extractBackground` override, `mouseClicked` takes a `MouseButtonEvent`, `imageWidth` and
`imageHeight` are final and go through the five-argument constructor, tooltips are deferred and automatic,
widgets draw before slots, and a sprite under `textures/gui/sprites` is drawn by id, nine-sliced by its
`.mcmeta`. `ClientMenu.screen` already registers a screen for a `MenuKey`; nothing in the new tree has used it,
and its type inference wants the screen's constructor as `StationScreen<BenchMenu>::new`, not a bare method
reference to a generic class. `ItemStack.onCraftedBy(player, count)` awards the crafted statistic and
`EventHooks.firePlayerCraftingEvent` posts NeoForge's event; the `recipe_crafted` criterion fires only for a
`RecipeCraftingHolder`, which no station is.

## The composite

`Station`, `@Composite(Dictionary.STATION)` in `composite/menu`, over `menu` and `log`. Its instance owns what
happens while modules are built: a consumer declares a `StationKind` and the composite checks the layout, refuses
a second declaration of the id, declares the kind's `MenuKey` to `menu` and marks the kind declared, so a bad
layout fails at startup rather than at the first click and a block entity built over an undeclared kind refuses
in its constructor naming the remedy. Everything that happens in a running game is done by the bases in the
same folder, which the game creates and calls and which, as actors, hold no module: they hold the kind, a
handle, as a menu holds its `MenuKey`, and they reach no module at all. What a feature must do with a module
(give the rest of a result through `inventory`, tell the player through `feedback`, award a statistic) it does in
its system, listening to the events the bases post; what needs no module (the station's own slots, a
`RecipeKey.find`, a `Fate.apply`, a progress counter) may stay in the block entity, as the map says an actor
runs its own mechanics and announces what happened. The composite takes no `Inventory` (rule 3), so reaching a
station through the capability lookups stays the consumer's one line, `inventory.exposeBlockEntity(type)`, as
the dev mod's bin does today.

| Piece | What it is |
|---|---|
| **Kind.** `StationKind<M extends StationMenu>`, a `@Handle` with a nested spec: `of(id, layout, title)` with a `LanguageKey` for the menu title and the block entity's default name, then `menu(factory)` for a menu subclass (the default builds the base `StationMenu`), `data(count)` for the ints the menu syncs, `syncedItems()` (its accessor is `sendsItems()`, since a no-argument wither of the same name cannot coexist with it) for a block entity whose item cells travel to clients (a renderer that shows them), `art(panel, slot)` for the screen's sprites. It builds and holds the `MenuKey<M>`, whose client side makes the client copy from the kind alone, a `SimpleContainer` of the layout's size and a `SimpleContainerData` of the count, reading nothing from the buffer; `create(windowId, inventory, container, data)` builds a menu on either side. A consumer keeps its kind as a constant in its `@Keys` set, since its block entity and menu name it before the system exists, and the system's constructor declares it. |
| **Layout.** `StationLayout`, a `@Value`: a panel 11 tiles (176 pixels) wide, the vanilla strip's width, `height` tiles high (at least 3), and an ordered list of `Cell`s, each a slot: pixel `x`, `y` inside the panel, a `Role` (`INPUT`, `TOOL`, `OUTPUT`, `STORAGE`), an `accepts` predicate (an output accepts nothing), and an optional `hint` sprite for the empty slot. The cell's position in the list is its container slot index and its saved slot number, so block entity, menu and screen agree by construction, no consumer indexes a slot twice, and cells are append-only once a station has shipped. Built by `StationLayout.of(height).input(tileX, tileY).tool(tileX, tileY, accepts, hint).output(tileX, tileY).storage(tileX, tileY).grid(rows, columns, left, top)` for an 18-pixel chest grid; `tile(n)` is the one helper for the 16-pixel grid, 1-based as the old `tile`. Checked when declared: every 18 by 18 frame inside the panel, no two frames overlapping, at least one cell, the cells of one role contiguous. The role decides everything else below, through methods on `Role`, never a switch in an actor. |
| **Block entity.** `StationBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer`, abstract, constructed with its kind: the item list sized by the layout, `getItems`, `setItems`, `getContainerSize`, `getDefaultName` from the kind's title; `canPlaceItem(slot, stack)` is the cell's role and predicate (an output refuses), the one statement of the rule that the menu slot reads too; every slot is reachable from every face, insertion through a face is `canPlaceItem` (a null side accepted, as `Inventory.exposeBlockEntity` asks), extraction through a face only from an `OUTPUT` or `STORAGE` cell, so a hopper under a bench takes results and never its inputs or tool, as it does under a furnace, while extraction with no side, the composite's own holders from within, is unrestricted. Save and load through `ContainerHelper` behind the loot-table check, the list re-created before loading so a removed stack does not linger; `createMenu(id, inventory)` builds the kind's menu over this block entity and `data(inventory.player)`, a read-only `ContainerData` of the kind's count answering `readData(index, viewer)`, which a kind with data overrides, per viewer, so a flag that depends on the opener's unlocks is right for each of two viewers; `createMenu` and `getDisplayName` stay overridable for a block entity that chooses its menu by state. `contentsChanged()` runs on the server when the cells differ from the snapshot the base keeps, compared after every `setChanged` and once on `onLoad`, so a cache is warm after a reload and a click that touches a slot three times refreshes once; by default it posts the `Changed` event. `onButton(player, id)` answers a menu button; by default it posts the `Button` event and answers whether a listener handled it. `deliver(cell, stack)` merges a result into one of the station's own cells regardless of its rule, since an output refuses everything else, and answers what did not fit. Sync: `writeSynced(output)` and `readSynced(input)` are what travels, by default nothing, the cells when the kind says `sendsItems()`, never the lock code; `getUpdateTag` writes it, `getUpdatePacket` sends it when it is not empty, `handleUpdateTag` and `onDataPacket` both read it, and `sync()` is `Sync.send(this)`. `dropsContents()` answers true; a storage whose item carries its contents answers false and `preRemoveSideEffects` then drops nothing, as the shulker box. `open(player)` is `player.openMenu(this)`: the block entity is the game's own provider, so the lock code, the custom name, the loot table and the spectator path behave as for a vanilla container; the vanilla lock now applies to every station, and a feature's own notion of locked keeps another name than `isLocked`. |
| **Block.** `StationBlock extends BaseEntityBlock`, abstract, with `codec()` and `newBlockEntity` left to the concrete block and `type()` naming its block entity type: `useWithoutItem` opens the block entity's menu on the server and answers `SUCCESS`; `getTicker` is `Tick.ticker(type, type())`; `affectNeighborsAfterRemoval` updates comparators and the block has the analog output of its contents, as every vanilla container block. Nothing drops contents: the game does, unless the block entity says otherwise. |
| **Menu.** `StationMenu extends AbstractContainerMenu`, concrete and open, constructed by the kind's factory with `(kind, windowId, inventory, container, data)`: it passes `kind.menu().type()` up, checks the container's size and the data count against the kind, adds one `StationSlot` per cell in order, then `addStandardInventorySlots(inventory, 8, panelHeight + 14)`, then the data slots; records `stationEnd`, `playerStart`, `hotbarStart`, `playerEnd`. `stillValid` is `valid(player)`, by default the container's (`Container.stillValidBlockEntity` on the server, true on the client copy). One `quickMoveStack`: an `OUTPUT` or `STORAGE` cell moves to the player backwards, the result and chest behaviour, an output then `onQuickCraft`; an `INPUT` or `TOOL` cell moves to the player forwards; a player slot tries the `TOOL` run, then `INPUT`, then `STORAGE`, one `moveItemStackTo` per contiguous run so a merge into a later partial stack wins over an earlier empty cell, the first run that takes anything wins, and when none does the main and hotbar ranges swap; `EMPTY` whenever nothing moved; the bench tail (`setByPlayer` or `setChanged`, `onTake`, the copy). `canTakeItemForPickAll` excludes output cells. `clickMenuButton(player, id)` asks the block entity's `onButton` when the container is a `StationBlockEntity` and the player a `ServerPlayer`, else false; `removed` returns nothing by default. `valid`, `clickMenuButton` and `removed` are overridable, so a menu over its own container (the old lock front: a one-slot `SimpleContainer` of the menu's own whose `setChanged` override is its change hook, chosen by the block entity's `createMenu` by state under another title, its data filled in `broadcastChanges`, its slot returned through `clearContainer` on close) builds on the base and supplies those three. `startOpen` and `stopOpen` on the container in the constructor and `removed`, no-ops unless a feature counts openers. Accessors for screens: the kind, the cells, a slot by cell index, `data(index)`, a `fraction(numerator, denominator)` helper for progress. |
| **Slot.** `StationSlot extends Slot`, built from a cell: `mayPlace` is the cell's rule, the hint is set as the slot's background sprite. For an `OUTPUT` cell it keeps vanilla's result bookkeeping (`remove`, `onQuickCraft`, `onSwapCraft`, the count), and on take fires `stack.onCraftedBy(player, count)` and `EventHooks.firePlayerCraftingEvent(player, stack, container)` with the station as the container, so a bench result counts in the crafted statistic and NeoForge's crafting event; the `recipe_crafted` criterion is a feature's own `triggerRecipeCrafted` from its listener. It is not a fake slot, its items are real. No change hooks: the block entity's `contentsChanged` is the one trigger. |
| **Events.** `StationEvent`, an `@Event` with `Button(player, station, id)` carrying a handled flag and `Changed(station)`, both posted on the game bus by the block entity, server only. A feature system listens, reads `event.station().kind()` to know its own, and does there what needs a module. |
| **Tick and Sync.** `Tick`, a `@Contract` with `tickServer(ServerLevel level)` for any block entity that ages, brews or polls, and `Tick.ticker(actual, expected)` the server-only ticker a block's `getTicker` answers, null on the client; `Sync`, a `@Helper` with `send(blockEntity)`, `setChanged` then `sendBlockUpdated` with `UPDATE_CLIENTS` on a server level. Both serve a plain block entity as well as a station: the hearth, barrel and cauldron, which have no items and no menu, use these two and nothing else of this composite, and a tick that must reach a module posts its feature's own event. No client tick: a lid animation is a feature's own `getTicker`. |
| **Screen.** `client/StationScreen<M extends StationMenu> extends AbstractContainerScreen<M>`: sized `176` by `panelHeight + 96` through the five-argument constructor, the inventory label at `panelHeight + 3`; `extractBackground` draws, after `super`, the panel and a well for every station slot read from the menu, and the vanilla strip from `generic_54.png` at `v = 126`; the panel and the wells are the kind's `art` sprites when it names them, else flat fills in the vanilla GUI greys, so the composite ships no texture; labels in opaque colours. Helpers for subclasses: `progress(sprite, x, y, width, height, fraction)`, the furnace's partial sprite blit, and `button(tileX, tileY, icon, id, active)`, a `StationButton` widget (a `Button` subclass drawing the vanilla button sprite under an item, greyed through the sprite when inactive since items take no tint) that sends `handleInventoryButtonClick(containerId, id)` and lets the server decide; both from the same `tile` helper as the layout, in free cells, since widgets draw under slots. The theme layer, the tool ghost tinting, the theme hooks and the per-pixel progress renderer are not carried: the empty-slot hint is the cell's sprite, the look is the feature's art, which arrives with the first feature. |

What stays with the features: the recipe lookup and the progress loop a tick or a button drives, in the block
entity when it needs no module; giving what `deliver` could not place through `inventory.give`, paying from the
station's cells, sounds, text, statistics and experience, in the system's listeners; lock sessions, lids and
opener counts, loot tables, custom block shapes and states, and every sprite.

## Lifecycle and rules

Built: a consumer's `@Keys` set holds its `StationKind` (and its `@Value` set the title key, so datagen demands
its English); its `@Content` registers the block and the block entity type; its constructor calls
`station.declare(kind)` and `inventory.exposeBlockEntity(type)`; the root adds the system's listeners for
`StationEvent.Button` and `Changed`; its client bootstrap calls `ClientMenu.screen(modBus, kind.menu(),
BenchScreen::new)`. Use, server thread, the game's own: a right-click opens the block entity as the provider; the
game syncs slots and data slots while the menu is open and polls `stillValid`; a hopper reads the roles; the
ticker runs `Tick`; a button or a change posts its event and the system acts with its modules. A client copy of
the menu knows the kind, its synced slots and data, nothing else; a screen reads only those. A block entity
calls no module ever; a listener may not call the inventory composite from inside the composite's own
transaction, which `inventory.md` states. Creative is the game's: results are taken as crafted, nothing here
pays. The dev mod's stations are the proof; no feature content comes with this.

## Proof

Unit tests: the layout's checks (a frame outside the panel, two frames overlapping, a height under 3, a role
split in two runs, the grid helper, `tile`, the cell order pinned), a kind's withers keeping its id and menu id, a
second declaration of an id refused, an undeclared kind refused by a block entity, `declared()`, the quick-move
route of roles as a pure function over cells. Scenes on the game-test server, isolated where they add a player,
with two stations of the dev mod's own: a bench (two inputs, a tool cell accepting one item with a hint, an
output, one data value that depends on the viewer, a button that moves one item from an input to the output and
toggles a block property, synced items, a `Tick` counter) and a crate (a 9 by 2 storage grid). Opening the bench
gives the player a menu with the station's slots then 36 player slots and tells the client the kind's type under
the title, the client copy building from an empty buffer; placing refused into the output and into the tool
cell by a wrong item, accepted into an input, the block entity's `canPlaceItem` agreeing with every menu slot;
shift-click from the hotbar landing in the tool cell first when accepted, else the first input, from an input
into the player strip, from the output backwards with one crafting event carrying the count and the crafted
statistic awarded, a stack no cell accepts swapping main and hotbar, nothing moving answering `EMPTY`; the crate's
storage moving backwards both ways; double-click gathering leaving the output alone; two viewers reading two
data values and each reaching its client copy on the next tick; a button click posting one `Button` event that
the dev root's listener handles, the handler's change reaching the slots, an unknown id answering false; one
`Changed` event for a menu click, one for a hopper insert, one for a capability insert, none for a save; the
property toggled keeping the contents, breaking the block closing the menu on the next tick and dropping them, a
block entity answering `dropsContents` false dropping nothing; a hopper above inserting into an input and never
the output, a hopper below extracting the output only, the exposed capability showing the same rules per side
and taking from any cell with no side; a save and load round trip keeping the items and dropping a removed one,
`onLoad` refreshing so a menu opened after the reload shows the cached state; `sync` sending the block entity
data packet with the cells to a tracking player and never the lock code, the crate sending none; the ticker
advancing the counter every server tick; the comparator reading the fill. The screen has no headless proof: the
author checks it in `runClient` with the dev stations, the first screen of the new tree, their sprites under
`src/gametest/resources`.

## Where things go, and the order

`composite/menu/Station`, `StationKind`, `StationLayout` (with `Cell` and `Role`), `StationBlockEntity`,
`StationBlock`, `StationMenu`, `StationSlot`, `StationEvent`, `Tick`, `Sync`, and `composite/menu/client/StationScreen`,
`StationButton`; `Dictionary.STATION`; both roots build it. The architecture test decides W10 with the first
actors: `checkActor` skips javac's synthetic members and allows, by shape, the game's immutable constant types
a block or block entity declares (state properties, voxel shapes, holders, codecs, identifiers). The dev mod gets
`gametest/module/composite/menu/TestStations` (two blocks, two block entities, keys, declared and exposed by its
root, a `Button` listener in its root, screens registered by its client bootstrap), `StationScenes`, blockstates
and sprites in `src/gametest/resources`. Two steps, one at a time: the composite, kind, layout, block entity,
block, menu, slot, events, tick and sync with their tests and scenes, and the W10 decision; then the screen, the
button and the progress helper, checked in the client. `module-plan.md` gets the `station` row when the first
step is built. A `CLAUDE.md` line with each step, mirrored into `AGENTS.md`.

## Choices made here, say if one is wrong

A composite over `menu`, not a system, because it is generic plumbing that is no feature and needs no composite;
the actors hold a `StationKind` and a `MenuKey`, handles, never the module, and speak to systems through two
events. The block entity base is vanilla's storage base, so loot tables come free and benches carry two null
fields. The panel is always 11 tiles wide. Roles drive the slot rule, the hopper faces, the quick-move route and
its direction, the pick-all exclusion and the result bookkeeping, so each rule is written once, in the layout. A
hopper takes only from output and storage cells, a change from the old benches; the mortar's pestle-only route
and the chest menus' refusal to swap are not kept. `contentsChanged` is the one refresh trigger, on a real
change; slot hooks are gone. Data is per viewer and read-only on the server. The menu is opened as
`player.openMenu(blockEntity)`, the block entity being the game's provider, so `MenuKey.open` is not on this path
and the client copy reads no position. Sync carries only what a kind lists, items on request, the lock never.
The result slot fires the crafting event with the station as its container. Buttons are real widgets. The
composite ships no texture: art is the feature's, the dev mod's sprites stay in its own resources.

## Decided 2026-10-28

From the challenge, folded in above: actors reach no module, so the bases post `Button` and `Changed` events and
feature code with modules lives in the system's listeners; the hearth, barrel and cauldron are not stations and
use only `Tick` and `Sync`, which serve any block entity; the base extends `RandomizableContainerBlockEntity`,
since implementing `RandomizableContainer` alone gives no loot generation; `dropsContents()` lets a storage whose
item carries its contents drop nothing; sync is `writeSynced` and `readSynced`, never the whole saved state, so
no locked contents reach other clients; `contentsChanged` compares the cells against a snapshot and runs on
`onLoad` too; `deliver(cell, stack)` replaces the copied output delivery; the lock front is a menu over its own
container through `valid`, `clickMenuButton` and `removed`, the block entity choosing it by state; quick-move
moves output and storage backwards and tries one role run at a time; the isLocked collision with the vanilla lock
is named; a block entity refuses an undeclared kind; extraction with no side is unrestricted; a bench result
counts as crafted but does not fire `recipe_crafted`; cells are append-only once shipped; the W10 decision comes
with the first actors; the composite ships no texture, by the rule that textures are feature content; the proof
gained the two-viewer, reload, property, crate, no-drop and event-count scenes. Refuted and kept as designed:
vanilla's spectator path, real widgets for buttons, the consumer's own exposure call, the missing-screen warning
the menu primitive already documents.
