# Review todo

Working list from the repository review of 2026-10-17. Numbers are stable: refer to an item as **R12**. Tick items off when fixed, or mark them `won't fix` with a reason; fixed items are removed from this file on cleanup (last cleanup 2026-10-23, the fixes are in the `Fixes` and `Review fix R..` commits). Line numbers are from the `Trades` branch on 2026-10-17 and will drift.

Status: 40 items open (38 review items, 2 balance items parked below). All Medium items are fixed. Everything compiles.

Working order: bugs first, then the 1.0 content, then balance. Content and balance findings are not review items.

## Low (38)

### Logic and gameplay

- [x] **R24. Two stacked RandomReplaceWithLootProcessors share the same per-position random, so the common archaeology gravel rate is 7% instead of 8%**  
  Done 2026-10-23: `RandomReplaceWithLootProcessor` derives its roll from `RandomSource.create(settings.getRandom(pos).nextLong() ^ seedSalt)`, where `seedSalt` is built from the loot table id and the probability in the constructor, so stacked instances roll independently while worldgen stays deterministic per position. A simulation by the reviewers gave 1.0% rare and 8.1% common. Structure NBTs are unchanged.  
  `JolCraftDwarvenFortressProcessors.java:226` · logic · `src/main/java/net/sievert/jolcraft/datagen/structure/processor/JolCraftDwarvenFortressProcessors.java`  
  Both processors draw their first nextFloat from settings.getRandom(current.pos()), which is seeded only by position. The two rolls are perfectly correlated, not independent.  
  Fix: Merge the two into one weighted processor that rolls once (rare, then common, then none), or mix a per-instance salt into the seed (for example RandomSource.create(settings.getRandom(pos).nextLong() ^ salt)).

- [ ] **R29. Sleep fast-forward only reaches block entities near players**  
  `BrewingSleepHandler.java:26` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/recipe/brewing/BrewingSleepHandler.java`  
  The sleep fast-forward deliberately covers only chunks within 4 chunks of an online player (a documented trade-off). Brews elsewhere, whether in a loaded chunk farther away or in an unloaded chunk, keep aging at the normal rate but miss the skipped-night bonus, so the result depends on distance.  
  Fix: Iterate all loaded chunks of the level (chunkMap visible chunks or the block entity tickers). Alternatively, track the skipped time in per-level saved data and apply it when the block entity loads.

- [ ] **R30. CropBlock light bypass cannot take effect below light 8, since CropBlock.canSurvive uses the same light check**  
  `CropBlockMixin.java:24` · logic · `src/main/java/net/sievert/jolcraft/mixin/CropBlockMixin.java`  
  The mixin raises the randomTick brightness to 9 on VERDANT soil. Vanilla CropBlock.canSurvive requires getRawBrightness >= 8, and the mixin does not touch that.  
  Fix: If dark farming on verdant soil is intended, also wrap the getRawBrightness call in CropBlock.hasSufficientLight (or override canSurvive for verdant). If not, drop the bypass and keep only the growth multiplier.

- [x] **R33. Lockpick break shrinks the slot stack without notifying the slot, leaving hasLockpickInserted stale**  
  Done 2026-10-23: both break branches of `StrongboxBlockEntity.handleLockButtonPress` check the slot after `shrink(1)`; when the last pick broke they call `setHasLockpickInserted(false)` (which runs `resetLockSession`) and return instead of rerolling, so the next inserted pick starts a fresh layout and timer.  
  `StrongboxBlockEntity.java:399` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/StrongboxBlockEntity.java`  
  When the last lockpick breaks, shrink(1) bypasses JolCraftSlot.setChanged, so hasLockpickInserted stays true. The buttons keep rerolling with an empty slot, and inserting the next pick skips the fresh reroll and timer reset. Progress is already zeroed and presses with an empty slot are rejected, so the effect is only cosmetic or timing.  
  Fix: After shrinking, set hasLockpickInserted = !lockpickSlot.isEmpty() and call resetLockSession() when it becomes false. Alternatively, have the menu call slot.setChanged() after the break.

- [x] **R34. currentInteractingPlayerId is set in createMenu and never cleared for StrongboxMenu, so a later lock session shows 'busy'**  
  Done 2026-10-23: `createMenu` sets the interacting id only on the `LockMenu` branch. The unlocked menu's opener is already tracked and cleared by the openers counter that the R15 fix wired (`StrongboxMenu` calls `startOpen`/`stopOpen`), so the two paths no longer both write the id.  
  `StrongboxBlockEntity.java:722` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/StrongboxBlockEntity.java`  
  createMenu (StrongboxBlockEntity.java:722) stores the opener's UUID even for the unlocked StrongboxMenu, and nothing clears it when that menu closes. If the box is later re-locked with the playtest-only DEV_KEY, other players get TOOLTIP_STRONGBOX_BUSY until the previous opener logs out. Normal survival play cannot re-lock an existing box. The finder's second consequence (ticking with the stale player's LOCKPICKING) does not happen, because a lock session also needs an open LockMenu with a lockpick inserted. Fix: set the id only in the LockMenu branch of createMenu.  
  Fix: Clear the id in a StrongboxMenu.removed() override (clearCurrentInteractingPlayer). Better, set the id only for the LockMenu branch of createMenu.

- [x] **R36. Reputation endorsement ids accept any namespace**  
  Done 2026-10-23: `isValidProfessionId` also requires the `jolcraft` namespace, matching how `DwarvenReputationAttachmentHelper` builds and looks endorsement ids up; foreign-namespace ids in a save are dropped on load with the existing debug log.  
  `DwarvenReputationAttachment.java:184` · logic · `src/main/java/net/sievert/jolcraft/world/entity/attachment/player/custom/reputation/DwarvenReputationAttachment.java`  
  isValidProfessionId ignores the namespace. A save edited offline can hold foreign-namespace endorsement ids, and the same profession can be stored under several namespaces. Each copy adds to getEndorsementCount and canAdvance, while hasEndorsement, which looks up the jolcraft namespace only, returns false. Normal gameplay never creates such ids.  
  Fix: Also require `JolCraft.MOD_ID.equals(id.getNamespace())` in isValidProfessionId.

- [x] **R48. Unknown profession id in trigger JSON silently decodes to NONE instead of failing**  
  Done 2026-10-23: both trigger codecs use `JolCraftTriggerCodecs.strictStringMapped` with `JolCraftEnumHelper.byStringIdNullable`, and both `TriggerInstance.CODEC`s are wrapped in the new `JolCraftCodecs.withoutPartials`. The wrapper matters: DataFixerUpper 8.0.16 gives a failed `optionalFieldOf` a partial empty Optional and advancement loading calls `promotePartial`, so without it an unknown profession in the trade trigger became an "any dwarf" wildcard (found and reproduced by the review against the real jar). With it the criterion fails and a single-criterion advancement is skipped with the usual parsing-error log. The literal `none` still decodes to NONE.  
  `DwarfEndorsementTrigger.java:69` · logic · `src/main/java/net/sievert/jolcraft/world/entity/player/advancement/custom/DwarfEndorsementTrigger.java`  
  The profession string codecs in DwarfEndorsementTrigger and DwarfTradeTrigger never reject an unknown profession id. DwarfProfession.byId falls back to NONE, so the null check is dead code. A misspelled id in a datapack advancement silently becomes NONE: the endorsement criterion can then never fire, and the trade criterion matches only NONE. Bundled datagen advancements always pass valid enum constants, so only hand-written or third-party datapacks are affected.  
  Fix: Use `JolCraftEnumHelper.byStringIdNullable(DwarfProfession.class, id, null)`, or reject `p == DwarfProfession.NONE`, in both trigger codecs (the same pattern is at DwarfTradeTrigger.java:62-66). JolCraftTriggerCodecs.strictStringMapped is already unused for this.

### Persistence and sync

- [x] **R52. Dwarf offer cost_b is read leniently, so a missing item silently drops the second cost**  
  Done 2026-10-23: `cost_b` is a plain `optionalFieldOf`, and `DwarfMerchantOffers.CODEC` wraps each element in `JolCraftCodecs.withoutPartials`. The wrapper is what makes the offer drop: DataFixerUpper 8.0.16 would otherwise still build the offer with an empty second cost as a partial result and `resultOrPartial` would keep it (reproduced by the review against the real jar). Now the offers list keeps the valid offers, drops the damaged one and the loader logs it; a restock or level-up rebuilds the missing offer.  
  `DwarfMerchantOffer.java:81` · persistence · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfMerchantOffer.java`  
  cost_b is decoded leniently, the same way vanilla decodes MerchantOffer buyB. If its item or component no longer exists (for example, after a mod is removed), the offer loads with only cost_a until the trades are rebuilt. Making the field strict without also decoding each list element separately would instead drop every offer on that dwarf.  
  Fix: Use optionalFieldOf so an invalid cost_b fails the offer, and drop only that offer (see next finding).

### Thread safety

- [x] **R55. JEI trade recipe creation reads the integrated server's ServerPlayer and ServerLevel from the client thread, and the result differs between singleplayer and multiplayer**  
  Done 2026-10-23: `JeiDwarfTradeRecipe` no longer builds a loot context. `amountRange` reads constant and uniform providers directly (exact value or range) and reports anything else as unknown, identically in single player and on a server; `resolveJeiLootContext`, the preview context params and the server imports are gone. All shipped trade costs are constant or uniform, so JEI output is unchanged for them.  
  `JeiDwarfTradeRecipe.java:571` · concurrency · `src/main/java/net/sievert/jolcraft/integration/jei/custom/dwarf_trade/JeiDwarfTradeRecipe.java`  
  The code path is real, but neither the claimed impact nor the medium severity holds up. resolveJeiLootContext (JeiDwarfTradeRecipe.java:571-610) is called for every trade recipe (line 147) and reads the integrated server's PlayerList and ServerPlayer from the client thread. However, only providers that are neither ConstantValue nor UniformGenerator ever use the context (lines 465-470, 493, 519). Every shipped trade cost is constant or uniform with constant bounds (374 uniform providers and nothing else in the generated dwarf_trade recipes). So the difference between singleplayer and a server only appears with third-party data packs. When it does, singleplayer shows a single fixed-seed sample as if it were an exact count, and a server shows "unknown". The cross-thread read is a single HashMap lookup by UUID. Its risk is negligible.  
  Fix: Do not build a loot context in JEI. Show a range for ConstantValue and UniformGenerator only, and "varies" for other providers, the same way on both sides. Remove the singleplayer-server access.

### Performance

- [x] **R59. Aging barrel copies the FluidStack (with data components) and recomputes age every tick**  
  Done 2026-10-23: `FermentingBarrelBlockEntity.tickServer` gates the `getCurrentBrew()` copy and the finished-aging commit behind the existing `AMBIENT_SOUND_INTERVAL` (200 ticks). Readers compute the current age from the timer and drains and fills commit it first, so the stored value lagging by up to ten seconds at the final threshold changes nothing the player sees.  
  `FermentingBarrelBlockEntity.java:289` · performance · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/brewing/FermentingBarrelBlockEntity.java`  
  For every non-empty barrel holding finished brew that can still age, tickServer calls getCurrentBrew(), which copies the stored FluidStack and applies addAgeInPlace on the copy. It does this every tick just to test canAgeFurther, then discards the copy.  
  Fix: Do the cheap gate first: if (level.getGameTime() % AMBIENT_SOUND_INTERVAL != 0L) return; and compute getCurrentBrew()/applyElapsedAge at that interval, or compute the remaining ticks until the next age threshold once and store it.

- [x] **R60. Full discovered-structure set is re-synced to the client on every addition but nothing client-side reads it**  
  Done 2026-10-23: `.sync(...)` removed from the `DISCOVERED_STRUCTURES` registration; every reader goes through `DiscoveredStructuresAttachmentHelper`, whose API takes `ServerPlayer` only. The attachment class keeps its stream codec (it still extends `JolCraftSyncedAttachment`) for a future client reader, which should then register a size-capped, owner-only sync.  
  `JolCraftAttachments.java:62` · performance · `src/main/java/net/sievert/jolcraft/world/entity/attachment/JolCraftAttachments.java`  
  DISCOVERED_STRUCTURES (JolCraftAttachments.java:71, not :62) is registered with the one-argument .sync(codec). On every discovery, login and tracking start, NeoForge sends the whole uncapped GlobalPos list to the owner and to every player tracking the owner. No client code reads it. That wastes bandwidth and leaks a player's discovered structure coordinates to nearby clients. Remove .sync(...), or if a client reader is planned, use sync((holder, to) -> holder == to, codec) with a size-capped list.  
  Fix: Remove `.sync(...)` for DISCOVERED_STRUCTURES if the client never reads it, or add a size-limited list codec if a client reader is planned.

- [x] **R61. Offer cost serializes the item's full default component map**  
  Done 2026-10-23: `DwarfTrades.createItemCost` builds the predicate from `PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, representative.getComponentsPatch())`, so only the representative's own patch (brew content, pouch amount, tablet data) is stored and sent; the display stack is unchanged because defaults come from the prototype, and matching never used the predicate (`DwarfItemCost.test` checks item, count and ingredient).  
  `DwarfTrades.java:214` · performance · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfTrades.java`  
  createItemCost builds the DataComponentPredicate with allOf(representative.getComponents()). That map includes every prototype default component, not only the recipe's required components.  
  Fix: Pass only the patch: DataComponentPredicate.allOf(DataComponentMap.builder() ...) built from representative.getComponentsPatch() additions. Or use DataComponentPredicate.EMPTY when the ingredient already enforces components.

- [x] **R62. Full recipe-manager scan and holder re-wrap on every offer refresh**  
  Done 2026-10-23: `DwarfTrades.getAllTradeRecipes` returns `recipeManager.getAllRecipesFor(DWARF_TRADE_TYPE)`, the type index, with one copy and no holder re-wrap; `getRecipeHolders` is gone. The three selectors still call it once each per refresh, which is now a map lookup.  
  `DwarfTrades.java:1042` · performance · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfTrades.java`  
  getAllTradeRecipes iterates RecipeManager.getRecipes() (all recipes) and type-filters instead of using the type-indexed getAllRecipesFor(type). Only matching DwarfTradeRecipes are re-wrapped and copied. It runs 1-2 times per dwarf per restock interval (default 6000 ticks), which is a minor, avoidable cost and not a tick-spike risk.  
  Fix: Use recipeManager.byType(type) / getAllRecipesFor(type), which is indexed by type, and cast. Drop the RecipeHolder re-wrap and the List.copyOf. Pass the list through restock() and rebuild instead of re-fetching it.

- [x] **R64. Corruption brewing recipe rebuilds the full output and rolls RNG on every brewing-stand tick**  
  Won't fix 2026-10-23 (accepted hot-path cost, as the finding itself allows): `IBrewingRecipe` has only `getOutput`, and NeoForge's `hasOutput` probe calls it, so the roll and stack build cannot be skipped for the per-tick `isBrewable` check without a result cache keyed on the slot stacks; that cache would fix the corruption effect at insertion time and carry stale-state and thread-safety risks for a saving of a few small allocations per active stand per tick. The cheap gates (`isInput` with `hasEligibleEffect`, `isIngredient`) already run first. Revisit only if profiling shows brewing stands matter.  
  `JolCraftCorruptionBrewingRecipe.java:85` · performance · `src/main/java/net/sievert/jolcraft/world/recipe/custom/vanilla/JolCraftCorruptionBrewingRecipe.java`  
  BrewingStandBlockEntity.serverTick calls isBrewable on every tick, which goes through hasMix and hasOutput to getOutput. JolCraftCorruptionBrewingRecipe.getOutput then does a full weighted roll and builds a discarded output stack. Because isBrewable stops at the first matching slot, this is usually about one build per stand per tick, not one per bottle slot.  
  Fix: This is a hot-path cost, not a correctness bug. Either accept it for now, or cache a cheap pre-check (hasEligibleEffect and the input checks) so the full roll only happens when the brew actually completes. A separate pure-predicate path would be needed, because IBrewingRecipe has no such hook.

### Resources, tags and ids

- [x] **R65. Block tag shears_loot is declared but never generated or used**  
  Done 2026-10-23: `JolCraftTags.Blocks.SHEARS_LOOT` and `JolCraftTagIds.SHEARS_LOOT` are deleted, with the two imports only they used. The tag translation came from `TagLangSubProvider` reflecting over the tag fields, so it disappears with the constant on the next `runData`.  
  `JolCraftTags.java:89` · resource-consistency · `src/main/java/net/sievert/jolcraft/data/JolCraftTags.java`  
  JolCraftTags.Blocks.SHEARS_LOOT (id built in JolCraftTagIds.SHEARS_LOOT) has no generated json and no code reference.  
  Fix: Delete the constant and the id, or populate it in JolCraftBlockTagProvider.
  Also reported as: Block tag SHEARS_LOOT is declared and given a translation but never populated or used (`JolCraftTags.java:89`).

- [ ] **R66. Legendary ancient tome base model uses the generic tome texture; ancient_dwarven_tome_legendary.png is orphaned**  
  `DwarfModelSubProvider.java:164` · resource-consistency · `src/main/java/net/sievert/jolcraft/datagen/client/model/subprovider/DwarfModelSubProvider.java`  
  generateLegendaryTomeModels hardcodes layer0 to item/book/tome/ancient_dwarven_tome for the base model. The shipped texture item/book/tome/ancient_dwarven_tome_legendary.png is referenced by no model, so it is dead.  
  Fix: If the base legendary art was intended, point layer0 at item/book/tome/ancient_dwarven_tome_legendary. Otherwise delete the orphan png.

- [x] **R67. Pestles are in durability_enchantable but not in c:tools, and the c:tools children include no mod melee or armor items**  
  Done 2026-10-23: `JolCraftTags.Items.PESTLES` is added to the `c:tools` builder in `JolCraftItemTagProvider`, next to spanners, hammers and chisels. The melee and armor half of the title was already refuted in the finding text. Needs `runData` to regenerate the tag json.  
  `JolCraftItemTagProvider.java:150` · resource-consistency · `src/main/java/net/sievert/jolcraft/datagen/tag/provider/JolCraftItemTagProvider.java`  
  Pestles (PestleItem extends ToolItem) are in minecraft:durability_enchantable but are missing from the c:tools builder at JolCraftItemTagProvider.java:150-153, while the other three custom tool tags are in it. The claim that the c:tools children include no mod melee or armor items is wrong: mod swords and warhammers reach c:tools through minecraft:swords, and armor does not belong in c:tools.  
  Fix: Add .addTags(JolCraftTags.Items.PESTLES) to the Tags.Items.TOOLS builder.

- [x] **R68. lastTradedPlayer keeps a strong reference to a Player and is never read**  
  Done 2026-10-23: the field and its single assignment in `rewardTradeXp` are removed from `AbstractTradingEntity`.  
  `AbstractTradingEntity.java:84` · resource-consistency · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/base/AbstractTradingEntity.java`  
  rewardTradeXp stores the ServerPlayer in a public field that is never cleared or used anywhere else.  
  Fix: Remove the field, or store a UUID or WeakReference.

- [x] **R69. JolCraftProgressRenderer.clearTextureCache is never called, so cached NativeImages go stale after a resource reload**  
  Done 2026-10-23: `JolCraftClientModEvents.registerReloadListeners` registers a `ResourceManagerReloadListener` on `RegisterClientReloadListenersEvent` that calls `clearTextureCache()`, which closes and drops the cached images; the renderer reloads them lazily on the next draw. The image map is the renderer's only static cache.  
  `JolCraftProgressRenderer.java:620` · resource-consistency · `src/main/java/net/sievert/jolcraft/world/gui/client/util/JolCraftProgressRenderer.java`  
  JolCraftProgressRenderer.IMAGE_CACHE is never cleared, because clearTextureCache() has no caller and no client reload listener is registered. A resource reload does not reach the Mortar progress textures, so they stay stale until restart. The off-heap memory it holds is small and fixed and does not grow.  
  Fix: Register a ResourceManagerReloadListener on the client (RegisterClientReloadListenersEvent) that calls clearTextureCache(). Alternatively, drop the cache by baking to a texture as in the previous finding.

### Build, CI and tests

- [ ] **R70. Gametest classes are loaded in runClient/runServer, but their mixin config is not registered there**  
  `build.gradle:71` · build-ci · `build.gradle`  
  In runClient, client2 and runServer the gametest source set is loaded and tests are registered, but the gametest mixin config is left out because inclusion depends on the task name. Running ExperimentalTests there (the only caller of JolCraftTestHelper.of) fails with a ClassCastException that is recorded as a test failure, not a crash, and packet recording does nothing. The other tests are unaffected.  
  Fix: Make the gametest mixin config unconditional for the gametest source set, for example by putting it in a gametest-only mods.toml or by listing it in a gametest resource. Or stop using the gametest source set for the interactive runs.

- [ ] **R71. Gametest mixin config is gated on task names and can leak into a shipped jar**  
  `build.gradle:274` · build-ci · `build.gradle`  
  Whether the gametest mixin config goes into the shared mods.toml depends on a substring match against requested task names. So any single Gradle invocation that runs a task with "gametest" in its name and also builds the jar produces a mod jar whose metadata references a missing jolcraft.gametest.mixins.json. CI is not affected because it runs `build` on its own.  
  Fix: Do not put the gametest mixin config in the shared mods.toml. Use a separate gametest-only generated mods.toml, or make 'jar' fail (or filter the [[mixins]] block) when includeGametestMixins is true.

- [ ] **R72. Loot-table validation is disabled for the whole mod**  
  `JolCraftMainLootTableProvider.java:53` · build-ci · `src/main/java/net/sievert/jolcraft/datagen/loot/table/JolCraftMainLootTableProvider.java`  
  JolCraftMainLootTableProvider.validate (lines 54-61) is a no-op, which turns off all vanilla datagen loot-table validation. That covers missing nested JolCraft-to-JolCraft references and param-set mismatches, not just the vanilla-reference false positives the comment means to skip.  
  Fix: Call super.validate and filter or ignore only the vanilla-reference problems, or validate against the set of generated JolCraft keys.

- [ ] **R73. Dev-only test hand_interaction recipe is generated into the shipped data**  
  `TestHandInteractions.java:73` · build-ci · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/hand/TestHandInteractions.java`  
  The dev test hand_interaction recipe (dev_key + dev_key -> diamond, emerald, cow, regeneration) is always generated into the shipped data and appears in recipe viewers. dev_key can only be obtained through the creative tab or /give, so it gives survival players no real exploit.  
  Fix: Gate the sub-provider behind a datagen flag or system property so release datagen does not emit it, or move it to a gametest-only data path.

### Code quality and dead code

- [ ] **R74. No LICENSE or README although the mod declares MIT**  
  `gradle.properties:20` · quality · `gradle.properties`  
  The mod declares MIT through mod_license (gradle.properties:20, copied into neoforge.mods.toml:3), but the repo and jar include no LICENSE file with the MIT text and copyright notice, and there is no README. Redistributors have no license text to include, but the terms are not unenforceable.  
  Fix: Add a LICENSE file with the MIT text and a short README covering build steps (runData first).

- [ ] **R75. Two game tests are fully commented out and the Experimental test is a scratch test that stays registered**  
  `ExperimentalTests.java:14` · quality · `src/gametest/java/gametest/tests/ExperimentalTests.java`  
  ChestLootGameTest and OreDensityOverworldTest are entirely commented out and are not in TEST_HOLDERS. ExperimentalTests is a registered scratch test ('Does whatever you want!') that prints to stdout and only asserts player presence.  
  Fix: Delete or revive the commented-out tests. Remove ExperimentalTests from TEST_HOLDERS, or disconnect the reconnected player in a finally block.

- [x] **R76. Generator debug log reports 10 server providers but 14 are registered**  
  Done 2026-10-23: the debug line in `JolCraftDataGenerator` now names all 14 server providers in registration order, with a comment to keep it in step with the `addProvider` calls.  
  `JolCraftDataGenerator.java:199` · quality · `src/main/java/net/sievert/jolcraft/datagen/JolCraftDataGenerator.java`  
  The 'Server providers' debug line lists only 10 names, while 14 server providers are added. The damage-type, entity-type, instrument and advancement providers are not listed.  
  Fix: Build the log from a list of the providers that were actually added, or add the missing names.

- [x] **R77. Duplicate COOKED_PORKCHOP entry doubles its weight in chest/supplies**  
  Done 2026-10-23: the second porkchop entry is now `COOKED_RABBIT`, the one vanilla cooked meat the pool lacked (beef, mutton, chicken, porkchop, cod, salmon were all present at weight 3, and rabbit stew sits two lines below), so the total weight is unchanged. Swap it for a plain removal if rabbit was not intended. Needs `runData`.  
  `JolCraftChestLootTableProvider.java:255` · quality · `src/main/java/net/sievert/jolcraft/datagen/loot/table/subprovider/JolCraftChestLootTableProvider.java`  
  Items.COOKED_PORKCHOP is added twice with weight 3 (lines 252 and 255), so its effective weight is 6 against 3 for every other cooked meat. This is almost certainly a copy-paste error.  
  Fix: Remove the duplicate, or replace it with the intended item (for example COOKED_RABBIT).

- [x] **R78. Builder state is not reset or validated consistently, and there is dead code**  
  Done 2026-10-23: the unused `LootPool` local in `result(ItemLike, min, max)` is deleted; `costA(TradeCost)` and `result(ItemOutput)` now reset their name token and coin flag before deriving them (the other cost and result setters already did). The subproviders never set a name token before the matching setter, so generated file names are unchanged.  
  `DwarfTradeRecipeBuilder.java:937` · quality · `src/main/java/net/sievert/jolcraft/datagen/recipe/builder/DwarfTradeRecipeBuilder.java`  
  result(ItemLike,min,max) builds a LootPool local that is never used, and shadows the `pool` field. Several setters leave stale naming metadata when called twice.  
  Fix: Delete the unused local. Reset the name and coin flags at the start of every costA, costB and result setter, or compute them lazily at build time.

- [ ] **R79. Every _left/_right shaped recipe pair is a duplicate, because vanilla shaped recipes already match mirrored patterns**  
  `ToolRecipesSubProvider.java:292` · quality · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/ToolRecipesSubProvider.java`  
  The tool recipes and misc lockpick, glass_mug and compass-dial recipes register an explicit mirror-image pattern as a second recipe. ShapedRecipePattern matches mirrored layouts automatically, so each pair is two recipes with identical inputs and outputs.  
  Fix: Emit a single pattern per tool. Asymmetric patterns are mirror-matched by vanilla already.

- [x] **R80. Inconsistent miner task recipe ids (bounty_novice vs crate_*)**  
  Done 2026-10-23: `DwarfMinerBountyTasks` has one `emitTier` with id = tier name, like the other three professions; the duplicate `emitBountyTier` is gone. The cartographer sound pair the four upper tiers already had is kept, so only the novice tier's second sound changes. Needs `runData`; the cache purges the old `bounty_novice` and `crate_*` files.  
  `DwarfMinerBountyTasks.java:219` · quality · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/bounty/task/DwarfMinerBountyTasks.java`  
  Miner task recipes get the ids bounty_novice and crate_<tier>, while every other profession uses plain <tier> ids. The cause is a duplicate emitBountyTier/emitTier pair that differs only in the id prefix and sound2. This is cosmetic: nothing looks the ids up, and the sounds have no shared convention across professions anyway.  
  Fix: Use a single emitTier that outputs id = tier name and one consistent sound pair, as the other three professions do.

- [x] **R81. Stone Skin degradation guard in the duration helper can never match (dead code)**  
  Done 2026-10-23: `isStoneSkinDegradation` and its check are removed from `JolCraftEffectDurationEventsHelper` (confirmed in the sources: `forceAddEffect`, which the degradation uses, posts no `MobEffectEvent.Added`, so the guard could only ever hit an unrelated re-add). The four empty damage hooks in `JolCraftEffectEvents` and their calls in `JolCraftDamageEventsHelper` are gone too.  
  `JolCraftEffectDurationEventsHelper.java:33` · quality · `src/main/java/net/sievert/jolcraft/event/game/world/entity/effect/util/JolCraftEffectDurationEventsHelper.java`  
  isStoneSkinDegradation never matches the case it was written for, because Stone Skin's level loss goes through forceAddEffect, which posts no MobEffectEvent.Added. It can only match an unrelated addEffect of Stone Skin one level lower with the same duration, and then it would wrongly skip Focus. JolCraftEffectEvents also keeps four empty damage hooks.  
  Fix: Remove the guard, or switch the degradation to addEffect and keep it. Delete the empty hook methods.

- [x] **R82. Dead code: unused compassExamples helper**  
  Done 2026-10-23: the method and the five imports only it used are deleted from `JeiHandInteractionHelper`; the JEI examples keep coming from `group.structures()`.  
  `JeiHandInteractionHelper.java:198` · quality · `src/main/java/net/sievert/jolcraft/integration/jei/custom/hand_interaction/JeiHandInteractionHelper.java`  
  The private helper compassExamples(...) at JeiHandInteractionHelper.java:198 is never called, because the JEI builder makes its examples from group.structures(). Datagen builds the structure tag from that same list, so the examples only differ from what the compass really targets if a datapack overrides the tag.  
  Fix: Delete the method, or use it in place of the group.structures() stream if the tag is the source of truth.

- [x] **R83. AbstractSyncingBlockEntity is dead code with a write-only hook**  
  Done 2026-10-23: the class is deleted. `SyncingBlockEntity` stays, since the barrel and cauldron implement it directly.  
  `AbstractSyncingBlockEntity.java:11` · quality · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/base/AbstractSyncingBlockEntity.java`  
  AbstractSyncingBlockEntity has no subclasses, so it is dead code, and Barrel and Cauldron repeat its boilerplate. The client is not left without data: NeoForge's default handleUpdateTag and onDataPacket pass the update tag to loadWithComponents/loadAdditional, so no paired load hook is needed.  
  Fix: Delete the class, or make Barrel and Cauldron extend it and add a paired loadClientData hook.

- [x] **R84. Dead no-op call in normalizeBrewingFluid**  
  Done 2026-10-23: the unused copy and the discarded `isSameFluidSameComponents` call are removed; the callers make their own pre-normalisation copies, as the finding noted.  
  `DwarvenBrewFluidHelper.java:346` · quality · `src/main/java/net/sievert/jolcraft/world/block/fluid/util/brewing/DwarvenBrewFluidHelper.java`  
  normalizeBrewingFluid (void) makes a copy at line 283 and then throws away the result of isSameFluidSameComponents at lines 346-349, so both are dead code. The callers in FermentingCauldronBlockEntity:709 and FermentingBarrelBlockEntity:539 already make their own pre-normalization copies, so no change-tracking is lost.  
  Fix: Remove the copy and the call. Alternatively, return the comparison result as a boolean 'changed' if callers need it.

- [x] **R85. Intermediate un-generated keys are registered in JolCraftLootTables.LOCATIONS**  
  Done 2026-10-23: the folder helpers in `JolCraftLootTables` take the path string and register only the foldered key, so `all()` holds exactly the generated tables. Every constant resolves to the same id as before.  
  `JolCraftLootTables.java:161`, `JolCraftLootTables.java:196` · quality · `src/main/java/net/sievert/jolcraft/world/loot/JolCraftLootTables.java`  
  Every constant wrapped with chest/strongbox/crate(key)/archaeology/fishing also registers its bare key in LOCATIONS (the themed crate overload does not). So all() would return keys that have no generated table. Nothing calls all() today, so this is latent.  
  Fix: Build the foldered key directly, without registering the intermediate one, and register only the final key.
  Also reported as: JolCraftLootTables registers phantom un-foldered ids and its all() set is wrong (`JolCraftLootTables.java:196`).

- [x] **R86. Recipe hook ids are not validated at load time**  
  Done 2026-10-23: `RecipeOutput.validate` (a default all four outputs reach) reports every hook id missing from `JolCraftRecipeHooks` (new `contains`). Every recipe codec funnels its outputs through `RecipeValidation.validateOutput`, so an unknown hook now fails the recipe at load and in datagen instead of logging on first use.  
  `JolCraftRecipeHooks.java:38` · quality · `src/main/java/net/sievert/jolcraft/world/recipe/base/output/hook/JolCraftRecipeHooks.java`  
  An unknown hook ResourceLocation in a recipe's hooks list passes codec and ValidationContext checks (ItemOutput.validate and RecipeOutput.validate never look at hooks), and only logs an error at execution, returning false.  
  Fix: In ItemOutput, EntityOutput, SoundOutput and EffectOutput validate(), call context.reportProblem for any hook id not in JolCraftRecipeHooks.HOOKS (expose a contains() method), and make the codecs fail with it.

- [x] **R87. DwarvenFortressStructure ignores its serialized start_height, so the generated config is misleading**  
  Done 2026-10-23: `spawnOrigin` samples `start_height` and adds it to the stone-column origin, so the field is live data; the generated absolute(0) means no shift and a constant provider consumes no RNG, so placement is unchanged. Documented at the datagen value.  
  `DwarvenFortressStructure.java:98` · quality · `src/main/java/net/sievert/jolcraft/world/worldgen/structure/custom/DwarvenFortressStructure.java`  
  startPos() is overridden to derive Y from the stone column, but the codec still requires a start_height field and the datagen writes absolute 0. The value is dead data.  
  Fix: Either make start_height optional in AbstractJigsawStructure.codec, or apply it as an offset in spawnOrigin. Document that it is unused.

### Found while fixing (added after the review)

- [x] **R88. corruption_effects.json flaps between datagen runs**  
  Done 2026-10-23: `JolCraftCodecs.STABLE_MOB_EFFECT_INSTANCE` wraps the vanilla codec and sorts `neoforge:cures` on encode; used by `EffectOutput`, `WeightedEffect` and the fermenting-container processor's `BrewEntry`, which covers the config, the cauldron recipes, the hand-interaction test recipe and the fortress barrel processor list. Residual: `loot_table/chest/supplies.json` carries brew effects through vanilla `PotionContents.CODEC` and can still flap; a generator-level sort belongs to the datagen overhaul (roadmap 3).  
  `CorruptionEffectsConfigProvider.java`, `CorruptionEffectsConfig.java:21` · build-ci · `src/main/java/net/sievert/jolcraft/datagen/config/subprovider/CorruptionEffectsConfigProvider.java`  
  The weighted effects are written through the full `MobEffectInstance` codec, which includes NeoForge's `cures` set (`milk`, `protected_by_totem`) stored in a hash set, so two `runData` runs produce different orderings of the same file. Harmless at runtime, but it defeats diffing generated output and will show up as noise in any datagen determinism check.  
  Fix: encode only what the config needs (effect id, duration, amplifier) in `WeightedEffect.CODEC`, or sort the cures before writing. Belongs to roadmap item 3 (datagen overhaul).  
  Seen 2026-10-20: the same `cures` ordering also flaps `recipe/fermenting_cauldron/*_effect_unfinished_brew.json` (4 files) and `recipe/hand_interaction/test/supported_outputs.json` between consecutive `runData` runs with no code change.

## Parked: balance (after the 1.0 content)

Working order agreed 2026-10-23: fix every bug above first, then implement the 1.0 content, then balance. These two are balance findings, not defects, so they wait for that pass. R8 (Blacksmith/Smelter levelling) was removed the same day as content work, not a review item.

- [ ] **R22. Merchant bounty items are mostly sold by the merchant for no more than the reward (arbitrage)**  
  `DwarfMerchantBountyTasks.java:60` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/bounty/task/DwarfMerchantBountyTasks.java`  
  Most NOVICE and APPRENTICE merchant bounty items are sold by the merchant in the same amounts for less than the expected coin reward. A player can buy the item, fill the bounty and redeem it for about +2-3 coins plus dwarf and player XP each time. The loop is not unlimited. Redeeming restocks only the bounty offer, while the item trades have 3 uses and come from a random pool. It continues only until those trades run out or a restock crate drops.  
  Fix: Draw task items from things the merchant does not sell, or scale rewards so that expected reward is below the merchant's sell price of the task items.

- [ ] **R23. Artisan buy prices and Merchant sell prices overlap, so coins can be farmed by choosing favourable rolls**  
  `DwarfArtisanTrades.java:62` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/dwarf_trade/DwarfArtisanTrades.java`  
  The per-unit price ranges overlap between Merchant buy offers and Artisan sell offers for quartz, lapis, emerald and amethyst, so with favourable independent rolls a player can turn a profit. The Merchant's 3 uses per restock cap it, so it is a minor balance problem and not a medium logic defect.  
  Fix: Make Artisan's maximum coins-per-unit strictly lower than the Merchant's minimum coins-per-unit for every shared item, for example by lowering Artisan's coin rewards or raising the Merchant's coin cost.

## Roadmap (author's list) with related review items

The author's own planned work, in the author's order. Review items that touch the same area are listed so they can be folded into that work instead of being fixed in isolation.

1. **Lang overhaul** — related: R65
2. **Tag cleanup (use common)** — related: R65, R67
3. **Datagen overhaul** — related: R72, R77, R79, R80, R73, R78, R66, R85, R24, R87
4. **Datagen logs even for normal startup** — related: R76
5. **Sound overhaul** — related: none left
6. **Advancements** — related: R48
7. **Config** — related: R60
8. **Capabilities** — related: R59, R83
9. **Commands** — related: none left
10. **Gametests** — related: R75, R71, R70, R72
11. **Integration** — related: R55, R82, R69

## Larger design items from the review (not single fixes)

- Strongbox menu lifecycle: wire startOpen/stopOpen/removed(), drop the per-tick recheck, clear the interacting-player id (R33, R34; R12 and R15 are fixed).
- One fail-soft policy for data-driven config and recipe validation (R86, R48; R6 and R17 are fixed).
- Obtainability and economy pass (balance, after the 1.0 content): Muffhorn spawn, Blacksmith/Smelter/Arcanist/Priest progression, Artisan vs Merchant price overlap, bounty arbitrage (R22, R23; R2 is fixed).
