# Review todo

Working list from the repository review of 2026-10-17. The full report (failure scenarios, evidence, the 26 refuted findings) is `jolcraft-review-2026-10-17.md` in the session scratchpad; ask Claude to re-send it. Numbers are stable: refer to an item as **R12**. Tick items off when fixed, or mark them `won't fix` with a reason. Line numbers are from the `Trades` branch on 2026-10-17 and will drift.

Status: 86 items remain from the review (13 medium, 71 low, 2 balance items parked below). No critical or high severity. Everything compiles.

Working order: bugs first, then the 1.0 content, then balance. Content and balance findings are not review items.

## Medium (13)

Ordered by impact as judged in the synthesis.

- [x] **R1. Deepslate and mithril repair tags are never generated, so gear cannot be repaired with its material**  
  Done 2026-10-17: `JolCraftMaterials.Material` now owns `repairItem()`, `repairTag()` and `repairIngredient()`; tool tiers and armor materials read from the material, and the item tag provider plus tag language subprovider iterate `Material.values()`, so a new material cannot ship without a populated repair tag.  
  `JolCraftArmorMaterials.java:41`, `JolCraftTags.java:53`, `JolCraftItemTagProvider.java:70` · resource-consistency · `src/main/java/net/sievert/jolcraft/datagen/tag/provider/JolCraftItemTagProvider.java`  
  No tag provider ever fills jolcraft:repairs_deepslate or jolcraft:repairs_mithril, so the repair Ingredient for every deepslate and mithril tool and armor material is empty. Anvil repair with the material never works, though nothing crashes and repairing by combining two items still works.  
  Fix: In JolCraftItemTagProvider.run add target.tag(JolCraftTags.Items.REPAIRS_DEEPSLATE).add(<deepslate repair item, e.g. the deepslate plate or ingot item>) and target.tag(REPAIRS_MITHRIL).add(JolCraftItems.MITHRIL_INGOT.get()). Use whatever item the recipes treat as the material.
  Also reported as: repairs_deepslate and repairs_mithril item tags are used as repair ingredients but never generated (`JolCraftTags.java:53`).
  Also reported as: Deepslate and mithril gear can never be repaired with materials (repair tags are empty) (`JolCraftArmorMaterials.java:41`).

- [x] **R2. Muffhorn is not obtainable in survival, but trades, bounties and recipes depend on its products**  
  Done 2026-10-18: `add_spawn_muffhorn` biome modifier (weight 5, groups 2-4) on `#jolcraft:mountains_and_hills`; `jolcraft:muffhorn_spawnable_on` block tag (= `#minecraft:goats_spawnable_on`); `MuffhornEntity.checkMuffhornSpawnRules` registered via `RegisterSpawnPlacementsEvent` (on ground, `MOTION_BLOCKING_NO_LEAVES`, daylight) in `JolCraftModEvents`.  
  Correction 2026-10-17: the Journeyman Merchant pooled trades (DwarfMerchantTrades.java:476-480, `pooledBuy` = coins for item) SELL fur and milk, so the products are buyable. Only the animal itself is unobtainable (no spawn placement, no biome modifier, not in any structure piece, egg only in creative).  
  `JolCraftModEvents.java:53` · registration · `src/main/java/net/sievert/jolcraft/event/mod/JolCraftModEvents.java`  
  The Muffhorn never spawns in survival: it has no spawn placement, no add_spawns biome modifier, is in none of the 201 structure NBTs, and its spawn egg is in no loot table or trade. So Muffhorn fur and milk can only come from creative or commands. That leaves two journeyman bounty pool entries impossible to complete, plus the fur-block recipe and the two milk-to-Inverix mortar recipes. Inverix itself can still be found in crate loot.  
  Fix: Add a neoforge:add_spawns biome modifier for jolcraft:muffhorn (for example in plains or mountain biomes) in JolCraftBiomeModifiers. Register SpawnPlacements.ON_GROUND with Animal.checkAnimalSpawnRules in a RegisterSpawnPlacementsEvent handler. Alternatively, spawn it in a structure, or give another way to obtain the fur and milk.

- [x] **R3. Unopened loot-table strongbox loses its loot when broken without Silk Touch**  
  Done 2026-10-18: `StrongboxBlock.getStrongboxDrops` calls `unpackLootTable(null)` in the unlocked non-silk branch (no-op while locked, matches vanilla container access).  
  `StrongboxBlock.java:191` · persistence · `src/main/java/net/sievert/jolcraft/world/block/custom/StrongboxBlock.java`  
  getStrongboxDrops reads the raw items list without calling unpackLootTable, so an unlocked strongbox with a pending loot table drops only the empty box and its loot is discarded.  
  Fix: In the unlocked non-silk branch, call strongbox.unpackLootTable(null) before iterating the items.

- [x] **R4. Weapon-based ATTACK_DAMAGE base is overwritten with the config default on every entity load**  
  Done 2026-10-20: removed the hardcoded per-item damage table, `setCustomAttackDamage`, the `setItemSlot` hook and the revenge-goal re-sync; removed `attack_damage` from the per-profession config defaults. `DwarfAttributes.BASE_ATTACK_DAMAGE` (1.0, player-like) is the shared base, set in `createBase` and re-applied in `applyTo` so older saves migrate; the held weapon's own attribute modifier supplies the rest (warhammer 16.5 instead of the previous double-counted 32). Verified by three adversarial reviewers; the shared base value is a balance knob (unarmed dwarves hit for 1.0 now, 3.0 before).  
  `AbstractDwarfEntity.java:196` · persistence · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/base/AbstractDwarfEntity.java`  
  readAdditionalSaveData calls setProfession, which calls DwarfAttributes.applyTo and resets the ATTACK_DAMAGE base to the config value (3.0). The weapon-derived base saved in the entity's attributes tag is lost.  
  Fix: After loading, or at the end of setProfession/applyTo, call setCustomAttackDamage(getAttackDamage()). Alternatively skip ATTACK_DAMAGE in DwarfAttributes.applyMap, or re-sync in readAdditionalSaveData after super.readAdditionalSaveData and setProfession.

- [x] **R5. Hand interaction consumes both inputs even when the recipe fails (deepslate compass assembly destroys compass and dial)**  
  Done 2026-10-20: `RecipeHook.canApply` precondition (default true), `JolCraftRecipeHooks.canApply`, `RecipeOutput.hooksCanApply` and `HandInteractionRecipe.hooksCanApply`; the handler refuses before cancelling or consuming (fail sound, cooldown, no actions). `DeepslateCompassHook` resolves compass, dial and target once for both the precondition and the output. Salvage misses still consume, as designed. Verified by three reviewers.  
  `JolCraftHandInteractionEvents.java:133` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/recipe/JolCraftHandInteractionEvents.java`  
  For survival players, the deepslate compass assembly recipe uses up one empty compass and one dial even when DeepslateCompassHook turns the output down (dial has no group, or no undiscovered structure within 20 chunks). The player gets nothing back except the fail sound. The cause is that the hand-interaction handler applies the CONSUME actions without checking whether any output was generated.  
  Fix: Apply the actions only when generatedAny is true. Alternatively, move the fail path to return early after playing the fail sound, before the actions and cooldown. Also decide whether a sound-only or effect-only output should count as success.

- [x] **R6. One malformed config json aborts the entire datapack reload**  
  Done 2026-10-20: `JolCraftCodecConfigManager.apply` and `CorruptionEffectsConfigManager.prepare` log and skip bad or unknown files (strict: any codec error skips the file); defaults cover skipped entries, matching `applySynced`. Verified by two reviewers.  
  `JolCraftCodecConfigManager.java:67`, `JolCraftCodecConfigManager.java:72` · logic · `src/main/java/net/sievert/jolcraft/config/base/JolCraftCodecConfigManager.java`  
  The config managers throw from the reload listener on any codec error, or on a dwarf_profession file whose path is not a known profession (any namespace counts). This rejects the whole datapack reload: /reload keeps the old data, and a dedicated server refuses to start without --safeMode. The bad entry should be logged and skipped instead.  
  Fix: Log the error and skip that entry, keeping the previous or default value, as applySynced() already does for unknown ids. Use codec.parse(...).resultOrPartial(err -> JolCraftLogs.error(...)) and continue. Do the same in CorruptionEffectsConfigManager.prepare(), falling back to CorruptionEffectsConfig.defaults().
  Also reported as: Any unrecognised file in the config directory fails the whole datapack reload (`JolCraftCodecConfigManager.java:72`).

- [x] **R7. Bounty crate accepts any stack of the item and returns fresh default stacks, enabling repair and component laundering**  
  Done 2026-10-20: both fill paths accept only stacks equal to a fresh default stack (`ItemStack.isSameItemSameComponents`). Verified: no current bounty objective uses an item whose default stack carries components, strongbox objectives still accept an empty unlocked box.  
  `BountyCrateItem.java:111` · logic · `src/main/java/net/sievert/jolcraft/world/item/custom/bounty/BountyCrateItem.java`  
  The bounty crate accepts any stack of the right item type, ignoring its components (:141, :203), and gives back fresh default stacks (:111, :253). Generated bounties ask for damageable tools (DwarfMerchantBountyTasks.java:146-148, 215-216), and a crate can be refilled after extraction (:145). So any player can fully repair those tools as many times as they like, while enchantments and names are silently lost.  
  Fix: Only accept pristine stacks (no damage, and components equal to the item's default prototype) in tryFillCrate and use(). Alternatively, refuse damageable items in item objectives at BountyTaskRecipe validation.

- [x] **R9. Item property lookup throws on the render thread for any unregistered lore key**  
  Done 2026-10-20: `JolCraftItemProperties.valueOrDefault()` returns 0.0F (base model) for an unregistered key and warns once per key; `LoreKey`, `CoinPouchAmount` and `RewardCrateTheme` use it in their render-path `value()`. `value()` still throws and is used only by datagen (model subproviders). Only `LoreKey` could actually hit it; the other two switched for consistency.  
  `LoreKey.java:59` · crash · `src/main/java/net/sievert/jolcraft/world/item/client/property/custom/LoreKey.java`  
  LoreKey.value() throws IllegalStateException on the render thread when dwarf_lore_key holds a non-blank value that is not one of the six registered lowercase legendary keys. Commands, datapacks or older saves can put such a value there, and the client then crashes every time it renders the saved stack. Everything the mod generates itself uses valid keys.  
  Fix: In the render-path Property.value(), return 0.0F for an unknown key. Add a non-throwing lookup such as JolCraftItemProperties.valueOrDefault(). Keep the throwing variant only for datagen. Apply the same to RewardCrateTheme and CoinPouchAmount.

- [x] **R10. Delirium sound wrapper drops tickable behaviour, so looping tickable sounds never stop**  
  Done 2026-10-20: `onSound` no longer wraps `TickableSoundInstance` or `SoundSource.RECORDS` sounds. The second case covers jukebox songs, which `LevelRenderer.stopJukeboxSong` stops by instance, so a disc started during Delirium kept playing after removal. Trade-off: minecart, bee, elytra and jukebox sounds are not muffled during Delirium. A `SoundEngine` mixin scaling volume/pitch would muffle everything without a wrapper; not done.  
  `JolCraftClientDeliriumEventsHelper.java:28` · logic · `src/main/java/net/sievert/jolcraft/event/client/game/effect/curse/JolCraftClientDeliriumEventsHelper.java`  
  The Delirium Scaled wrapper (JolCraftClientDeliriumEventsHelper.java:32/51) implements only SoundInstance. SoundEngine therefore never ticks it or checks isStopped(), so looping tickable sounds such as minecart and bee sounds freeze in place and never stop. Because instanceToChannel is keyed by the wrapper, a later stop(original) call cannot find the sound, so it keeps playing.  
  Fix: Skip wrapping when sound instanceof TickableSoundInstance, or add a second wrapper that implements TickableSoundInstance and forwards tick(), isStopped() and canStartSilent(). Alternatively, mutate the volume and pitch of AbstractSoundInstance in place.

- [x] **R11. DarknessPredicate reads light during the features step, when sky light is always 15, so deepslate bulbs never place**  
  Done 2026-10-20: `DeepslateBulbsCropBlock.hasSufficientDarkness` checks `y < WORLD_SURFACE` height when the level is a `WorldGenRegion` (light is not computed yet), and light ≤ 8 otherwise. This was a second blocker: `SimpleBlockFeature` calls `canSurvive`, which made the same light read. Deleted `DarknessPredicate`, the then-empty `JolCraftBlockPredicateTypes` registry and `JolCraftBlockPredicateTypeIds`, and `JolCraftDictionary.BRIGHTNESS`; the placed feature keeps only the plantable-tag filter.  
  `DarknessPredicate.java:29` · logic · `src/main/java/net/sievert/jolcraft/world/worldgen/predicate/custom/DarknessPredicate.java`  
  The predicate calls level.getRawBrightness at feature-generation time, before the chunk has been lit (INITIALIZE_LIGHT and LIGHT come after FEATURES). Sky light for an unlit column therefore reads as 15, so the '<= 8' test fails.  
  Fix: Do not use the light engine during generation. Test darkness structurally: for example, require no sky access with a heightmap or a WORLD_SURFACE_WG comparison, and use y < some bound or a solid-ceiling scan. Alternatively, check at a stage where light exists, or drop the predicate.

- [x] **R12. Every strongbox re-queries nearby players and broadcasts a block-event packet every tick**  
  Done 2026-10-20 together with R15 (vanilla chest pattern): `StrongboxMenu` calls `startOpen` in its server-side constructor and `stopOpen` in `removed()`; `StrongboxBlock.tick()` calls `recheckOpen()`, which the opener counter schedules every 5 ticks while open; the per-tick call in `tickServer` is gone. Closing the menu now also clears `currentInteractingPlayerId` for that player.  
  `StrongboxBlockEntity.java:820` · performance · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/StrongboxBlockEntity.java`  
  StrongboxBlockEntity.tickServer calls recheckOpen() every server tick, unconditionally. Each call runs a player AABB query and broadcasts a redundant block event 1 packet to all players within 64 blocks, even when the open count is 0 and unchanged. StrongboxBlock has no scheduled tick() override, so the fix needs to override tick() to call recheckOpen() and drop the per-tick call (or guard it with openerCount > 0).  
  Fix: Remove recheckOpen() from tickServer. Vanilla's ChestBlock/BarrelBlock instead override tick(...) (the scheduled block tick) to call recheckOpeners, and incrementOpeners schedules it. Override StrongboxBlock.tick(state, level, pos, random) to call the block entity's recheckOpen(). Alternatively guard the call with openersCounter.getOpenerCount() > 0 and only signal when the count changes.

- [x] **R13. Rejuvenation is registered as an OverhealEffect and refills the overheal bar**  
  Done 2026-10-20: `REJUVENATION` uses a plain `RejuvenationEffect` (combat/defence). The refill only mattered with another `MAX_OVERHEAL` source (Emberglass trims, an active Overheal).  
  `JolCraftEffects.java:391` · logic · `src/main/java/net/sievert/jolcraft/world/entity/effect/JolCraftEffects.java`  
  REJUVENATION (a heal-increase potion) is constructed with new OverhealEffect(...), so its onEffectStarted tops the overheal bar up to the maximum.  
  Fix: Use a plain MobEffect subclass (for example a new RejuvenationEffect) for REJUVENATION, so the overheal refill logic only runs for OVERHEAL.

- [x] **R14. DISARMED has no effect on dwarf melee because DwarfAttackGoal bypasses the mixed-in method**  
  Done 2026-10-20: `JolCraftEquipmentHelper.isMeleeBlocked` (Disarmed or Stunned while holding a weapon) is shared by `MeleeAttackGoalMixin`, `DwarfAttackGoal.canPerformAttack` and the hit in `DwarfRevengeGoal` (Stunned already cleared the attack goal's target, but revenge hits got through). JolCraft swords, warhammers and axes added to `c:tools/melee_weapon`.  
  `DwarfAttackGoal.java:128` · mixin · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/ai/goal/dwarf/DwarfAttackGoal.java`  
  DISARMED (and the mixin weapon gate) has no effect on dwarf melee. DwarfAttackGoal.tick and DwarfRevengeGoal.tick deal damage directly and never go through MeleeAttackGoal.checkAndPerformAttack. On top of that, JolCraft's own weapons (deepslate axe and warhammer, mithril warhammer) are not in c:tools/melee_weapon, so isWeapon() is false for them. The fix needs both a DISARMED/STUNNED check in the dwarf attack path and those items added to the melee-weapon tag, or a different weapon check.  
  Fix: In DwarfAttackGoal.canPerformAttack and DwarfRevengeGoal.tick, skip the attack when mob.hasEffect(DISARMED) or STUNNED and JolCraftEquipmentHelper.isWeapon(mainhand). Better, move the check into AbstractDwarfEntity.doHurtTarget.

- [x] **R15. Strongbox lid animation and open/close sounds are dead: startOpen/stopOpen are never called**  
  Done 2026-10-20 with R12. The premise was partly wrong: the per-tick `recheckOpen()` (R12) recounted players with the menu open, so the lid and sounds did work; removing it without this fix would have broken them. `LockMenu` stays out of the counter (`isOwnContainer` counts only `StrongboxMenu`).  
  `StrongboxBlockEntity.java:545` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/StrongboxBlockEntity.java`  
  Nothing calls StrongboxBlockEntity.startOpen/stopOpen: StrongboxMenu's constructor doesn't and it has no removed() override. The opener counter therefore never changes, so the strongbox lid never animates open and its open/close sounds never play. The lock and lockpick logic is unaffected because it sets currentInteractingPlayerId separately.  
  Fix: Call blockEntity.startOpen(inventory.player) in the StrongboxMenu constructor, but only on the server (!level.isClientSide). Override StrongboxMenu.removed(Player) to call blockEntity.stopOpen(player). Do the same in LockMenu if the lid should react to lockpicking.

- [x] **R16. JolCraftProgressRenderer draws the arrow with one GuiGraphics.fill per pixel every frame**  
  Done 2026-10-20: `renderInternal` wraps `renderArrow` in `GuiGraphics.drawManaged`, so the per-pixel fills batch into one flush instead of one draw call each. The per-pixel CPU blend is unchanged. `drawManaged` is deprecated in 1.21.1 but vanilla uses it for tooltip backgrounds.  
  `JolCraftProgressRenderer.java:486` · performance · `src/main/java/net/sievert/jolcraft/world/gui/client/util/JolCraftProgressRenderer.java`  
  JolCraftProgressRenderer.renderSegment (lines 434-543) calls GuiGraphics.fill once per opaque pixel of the 114x16 Mortar progress bar every frame, and blends the theme colours on the CPU each time. Because vanilla fill flushes the buffer when rendering is unmanaged (GuiGraphics.java:319), that means up to about 1800 separate draw calls per frame while the Mortar screen is open.  
  Fix: Bake the empty and filled composites (arrow plus theme blend) once into a DynamicTexture or sprite when the texture cache is built. Then blit clipped regions per frame. If that is too much, at least cache the blended ARGB pixels in an int[] per width instead of recomputing them.

## Low (71)

### Crashes

- [x] **R17. Reward crate loot table construction throws on duplicate pool names**  
  Done 2026-10-20: `Serializer.validate` tracks non-null pool names across `rewards` and rejects the recipe with `rewards[i] duplicates pool name '...'`, so the error surfaces at recipe load instead of as a server exception in `finishUsingItem`. Generated recipes never name pools.  
  `BountyRewardRecipe.java:128` · crash · `src/main/java/net/sievert/jolcraft/world/recipe/custom/bounty/BountyRewardRecipe.java`  
  createLootTable() calls NeoForge's LootTable.addPool per reward. That throws a RuntimeException if two pools share a non-null name, and recipe validation does not check this.  
  Fix: Reject duplicate non-null pool names in Serializer.validate, or build the table with LootTable.Builder, which does not check names.

### Logic and gameplay

- [x] **R18. Festerling crop drops nothing at ages 1-2 and no seed or flesh at age 3**  
  Done 2026-10-20, intended design per the author: the planted rotten flesh is never refunded, at any age. Removed the age 0 rotten flesh pool, so the crop drops nothing until age 3, where it drops a festerling.  
  `JolCraftBlockLootTableProvider.java:132` · logic · `src/main/java/net/sievert/jolcraft/datagen/loot/table/subprovider/JolCraftBlockLootTableProvider.java`  
  The festerling crop loot table only refunds rotten flesh at age 0, so a crop broken at age 1 or 2 drops nothing and the planted rotten flesh is lost. The flesh pool should be conditioned on age < 3, not age == 0. The age 3 festerling-only drop is intended.  
  Fix: Make the rotten flesh pool unconditional, or condition it on age < 3 (inverted condition). Keep the age 3 pool for the festerling.

- [x] **R19. Coin detection by item uses registry-holder tags, which are unbound during datagen**  
  Done 2026-10-20: `DwarfTradeRecipeBuilder.isCoinItem` matches gold coin / coin pouch by item, and `isCoinCost` inspects the ingredient's values for the coins tag or a coin item; the five generic setters use these instead of `builtInRegistryHolder().is(COINS_TAG)` / `TradeCost.contains()`. Generated recipes are unchanged.  
  `DwarfTradeRecipeBuilder.java:556` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/builder/DwarfTradeRecipeBuilder.java`  
  The generic costA/costB/result setters that take an ItemLike or a TradeCost (DwarfTradeRecipeBuilder.java:556, 624, 727, 786, 977) check for coins with builtInRegistryHolder().is(COINS_TAG) or TradeCost.contains(), which calls ingredient.getItems(). Item tags are never bound during runData, so both checks always return false. A trade written as costA(GOLD_COIN) or costA(coinsAsCost(..)) would be named TRADE instead of BUY, and the TradeCost version would also fail with 'has no filename token'. Every current caller uses the coin-specific setters, so this cannot happen yet.  
  Fix: Compare the item against DwarfTradeRecipe.GOLD_COIN or the coin item ids directly. For TradeCost, carry a coin flag with the cost or require callers to use the coin-specific setters.

- [x] **R20. Mortar has no peony recipe although the other dye flowers have one**  
  Done 2026-10-20: added peony → 4 pink dye (grinding work 20, tool damage 1) after the pink petals recipe, matching lilac, rose bush and sunflower.  
  `MortarRecipesSubProvider.java:273` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/MortarRecipesSubProvider.java`  
  Items.LILAC, ROSE_BUSH and SUNFLOWER (the double-tall flowers) are ground for 4 dye, but Items.PEONY has no recipe.  
  Fix: Add MortarRecipeBuilder.create().input(Items.PEONY).result(Items.PINK_DYE, 4).grindingWork(20).toolDamage(1).

- [x] **R21. Wooden and stone tool recipes accept only oak planks and plain cobblestone**  
  Done 2026-10-20: `artisanHammer`, `chisel`, `pestle` and `spanner` in `ToolRecipesSubProvider` now have an `Ingredient` core with a separate unlock item (the `ItemLike` overloads delegate to it). The wooden tier keys on `#minecraft:planks` and the stone tier on `#minecraft:stone_tool_materials`; unlocks stay on oak planks / cobblestone.  
  `ToolRecipesSubProvider.java:83` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/ToolRecipesSubProvider.java`  
  The wooden and stone artisan hammer, chisel, pestle and spanner recipes accept only oak planks and cobblestone, where vanilla tools accept any plank and any stone tool material. This is an inconsistency and a convenience gap, not a progression blocker: oak and cobblestone are widely available, and the iron and higher tiers need only vanilla materials.  
  Fix: Change the helper parameter type to a TagKey or Ingredient and pass ItemTags.PLANKS and ItemTags.STONE_TOOL_MATERIALS. Keep unlockedBy on a representative item.

- [ ] **R24. Two stacked RandomReplaceWithLootProcessors share the same per-position random, so the common archaeology gravel rate is 7% instead of 8%**  
  `JolCraftDwarvenFortressProcessors.java:226` · logic · `src/main/java/net/sievert/jolcraft/datagen/structure/processor/JolCraftDwarvenFortressProcessors.java`  
  Both processors draw their first nextFloat from settings.getRandom(current.pos()), which is seeded only by position. The two rolls are perfectly correlated, not independent.  
  Fix: Merge the two into one weighted processor that rolls once (rare, then common, then none), or mix a per-instance salt into the seed (for example RandomSource.create(settings.getRandom(pos).nextLong() ^ salt)).

- [ ] **R25. Crop loot bonus pops extra drops during BlockEvent.BreakEvent before the break is confirmed**  
  `JolCraftPlayerAttributeEventsHelper.java:288` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/player/attribute/JolCraftPlayerAttributeEventsHelper.java`  
  applyCropLootIncrease spawns bonus item entities from the cancellable BreakEvent. It re-rolls Block.getDrops and does not wait for the break to succeed.  
  Fix: Run at EventPriority.LOWEST and re-check !event.isCanceled(), or hook the actual drops (a loot modifier or BlockDropsEvent) instead of BreakEvent.

- [ ] **R26. Festerling planting handler places a block without permission or interaction checks**  
  `JolCraftPlantingEventsHelper.java:47` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/player/util/JolCraftPlantingEventsHelper.java`  
  The festerling planting handler places the crop without checking spectator mode, adventure mode (mayBuild/canPlaceOn) or the event's useItem=DENY. Spectators and adventure-mode players can therefore plant festerlings, and a protection mod that only denies item use is bypassed. Vanilla spawn protection on the clicked block is already enforced upstream, and protection mods that cancel the event stop it from reaching this handler.  
  Fix: Gate on event.getEntity().mayUseItemAt(cropPos, event.getFace(), event.getItemStack()) and !player.isSpectator(), and validate with canSurvive before placing.

- [x] **R27. Hand-interaction cooldown is applied to Items.AIR when an input stack was fully consumed**  
  Done 2026-10-20 with R5: both `Item` references are captured before the actions run and used by `applyCooldowns`.  
  `JolCraftHandInteractionEvents.java:145` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/recipe/JolCraftHandInteractionEvents.java`  
  main and off are the live stacks. After the actions shrink one to empty, ItemStack.getItem() returns Items.AIR for an empty stack, so addCooldown targets AIR instead of the real item.  
  Fix: Capture Item mainItem = main.getItem() and Item offItem = off.getItem() before applying the actions, and use those for the cooldowns.

- [ ] **R28. Block conversion ignores event.getUseBlock()/mayBuild and swaps blocks for players who cannot build**  
  `BrewingBlockConversionEventsHelper.java:37` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/recipe/brewing/BrewingBlockConversionEventsHelper.java`  
  tryHandle converts vanilla water cauldrons and barrels and moves their contents even when another handler has set useBlock to DENY, when the player is sneaking with an item in hand, or when the player is a spectator. In all three cases vanilla would skip the block's useItemOn. The adventure-mode/mayBuild part is not a real difference, because vanilla block use ignores mayBuild.  
  Fix: Return early when event.getUseBlock() == TriState.FALSE or event.getEntity().mayBuild() is false. Also return when the player is crouching with a non-empty hand (event.getEntity().isSecondaryUseActive()).

- [ ] **R29. Sleep fast-forward only reaches block entities near players**  
  `BrewingSleepHandler.java:26` · logic · `src/main/java/net/sievert/jolcraft/event/game/world/recipe/brewing/BrewingSleepHandler.java`  
  The sleep fast-forward deliberately covers only chunks within 4 chunks of an online player (a documented trade-off). Brews elsewhere, whether in a loaded chunk farther away or in an unloaded chunk, keep aging at the normal rate but miss the skipped-night bonus, so the result depends on distance.  
  Fix: Iterate all loaded chunks of the level (chunkMap visible chunks or the block entity tickers). Alternatively, track the skipped time in per-level saved data and apply it when the block entity loads.

- [ ] **R30. CropBlock light bypass cannot take effect below light 8, since CropBlock.canSurvive uses the same light check**  
  `CropBlockMixin.java:24` · logic · `src/main/java/net/sievert/jolcraft/mixin/CropBlockMixin.java`  
  The mixin raises the randomTick brightness to 9 on VERDANT soil. Vanilla CropBlock.canSurvive requires getRawBrightness >= 8, and the mixin does not touch that.  
  Fix: If dark farming on verdant soil is intended, also wrap the getRawBrightness call in CropBlock.hasSufficientLight (or override canSurvive for verdant). If not, drop the bypass and keep only the growth multiplier.

- [ ] **R31. Any player can extinguish another player's lit hearth with a water bucket (no owner check)**  
  `HearthBlockEntity.java:67` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/HearthBlockEntity.java`  
  Lighting a hearth is gated by isOwner, but the LIT branch lets any player with a water bucket (or any creative player) call deactivate(), which clears the owner and the owner's active hearth.  
  Fix: Require owner == null || isOwner(player) (or creative) before deactivating, or show the TOOLTIP_HEARTH_OWNER message to non-owners.

- [ ] **R32. Lapidary bench accepts any item into output and tool slots via hoppers**  
  `LapidaryBenchBlockEntity.java:43` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/LapidaryBenchBlockEntity.java`  
  LapidaryBenchBlockEntity has no canPlaceItem override, so hoppers can put any item into the tool and output slots. Nothing jams: junk in the output slot just sends results to the player inventory (or drops them), and a junk tool matches no recipe.  
  Fix: Override canPlaceItem: return false for SLOT_OUTPUT, require the ARTISAN_HAMMERS or CHISELS tag for SLOT_TOOL, and allow any item for SLOT_INPUT (or restrict it to GEODES/GEMS_UNCUT).

- [ ] **R33. Lockpick break shrinks the slot stack without notifying the slot, leaving hasLockpickInserted stale**  
  `StrongboxBlockEntity.java:399` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/StrongboxBlockEntity.java`  
  When the last lockpick breaks, shrink(1) bypasses JolCraftSlot.setChanged, so hasLockpickInserted stays true. The buttons keep rerolling with an empty slot, and inserting the next pick skips the fresh reroll and timer reset. Progress is already zeroed and presses with an empty slot are rejected, so the effect is only cosmetic or timing.  
  Fix: After shrinking, set hasLockpickInserted = !lockpickSlot.isEmpty() and call resetLockSession() when it becomes false. Alternatively, have the menu call slot.setChanged() after the break.

- [ ] **R34. currentInteractingPlayerId is set in createMenu and never cleared for StrongboxMenu, so a later lock session shows 'busy'**  
  `StrongboxBlockEntity.java:722` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/StrongboxBlockEntity.java`  
  createMenu (StrongboxBlockEntity.java:722) stores the opener's UUID even for the unlocked StrongboxMenu, and nothing clears it when that menu closes. If the box is later re-locked with the playtest-only DEV_KEY, other players get TOOLTIP_STRONGBOX_BUSY until the previous opener logs out. Normal survival play cannot re-lock an existing box. The finder's second consequence (ticking with the stale player's LOCKPICKING) does not happen, because a lock session also needs an open LockMenu with a lockpick inserted. Fix: set the id only in the LockMenu branch of createMenu.  
  Fix: Clear the id in a StrongboxMenu.removed() override (clearCurrentInteractingPlayer). Better, set the id only for the LockMenu branch of createMenu.

- [ ] **R35. World mutation inside BlockEntity.setLevel during chunk construction**  
  `FermentingCauldronBlockEntity.java:914` · logic · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/brewing/FermentingCauldronBlockEntity.java`  
  When a chunk loads from disk, setLevel() runs inside LevelChunk.setBlockEntity before the block entity is put into the chunk's map. If it calls restoreVanillaCauldron or restoreVanillaBarrelIfEmpty there, the swap to the vanilla block happens first and then the stale Fermenting BE is put in anyway. That leaves a ghost BE: it sits over a vanilla cauldron, or replaces the new vanilla BarrelBlockEntity, until the chunk is saved and loaded again. It does not cause a stall or a write into the wrong chunk.  
  Fix: Do not touch the world from setLevel. Only set a flag there, and perform the block replacement and offline completion on the first tickServer(). The barrel (setLevel -> restoreVanillaBarrelIfEmpty) should get the same fix.

- [ ] **R36. Reputation endorsement ids accept any namespace**  
  `DwarvenReputationAttachment.java:184` · logic · `src/main/java/net/sievert/jolcraft/world/entity/attachment/player/custom/reputation/DwarvenReputationAttachment.java`  
  isValidProfessionId ignores the namespace. A save edited offline can hold foreign-namespace endorsement ids, and the same profession can be stored under several namespaces. Each copy adds to getEndorsementCount and canAdvance, while hasEndorsement, which looks up the jolcraft namespace only, returns false. Normal gameplay never creates such ids.  
  Fix: Also require `JolCraft.MOD_ID.equals(id.getNamespace())` in isValidProfessionId.

- [ ] **R37. Dispenser shearing consumes shears but does nothing**  
  `MuffhornEntity.java:134` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/creature/MuffhornEntity.java`  
  onSheared returns an empty list without setting the sheared state when player is null. isShearable still returns true, so a dispenser's shears are used up for no result.  
  Fix: Remove the null-player early return. Make the sound play only when player is non-null, or play it at the entity's position.

- [x] **R38. Stop-time failures consume the player's item with no refund**  
  Done 2026-10-23: `PromoteDwarfAction.transformToProfession` rolls back (hand restored, contract refunded) when the contract has no mapping or the entity cannot be created, and on an `addWithUUID` failure revives and re-adds the original dwarf before refunding; the payment is charged on the promoted copy only. `BountyRewardAction.stop` resolves the recipe before touching the hand and rolls back when it, or the player, is missing. Rollback is `DwarfActionHelper.rollback`, shared with `stopAction`.  
  `BountyRewardAction.java:237`, `PromoteDwarfAction.java:132` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/action/type/profession/PromoteDwarfAction.java`  
  PromoteDwarfAction.stop() and BountyRewardAction.stop() have failure branches that leave the consumed contract or bounty without a refund. These are a datapack-added contract tag entry with no mapping, a bounty recipe removed by /reload during the 2-second window, and, for promote, a third-party mod cancelling EntityJoinLevelEvent after the original dwarf was discarded. None can happen with the shipped data. The discard-before-add order is needed to free the UUID, so on add failure the fix is to re-add the original dwarf or refund the contract, not to reorder the calls.  
  Fix: On every failure branch, refund via throwItem(dwarf, player, getActionInput()), or via the helper's recover path, and restore previousMainHandItem. In Promote, call discard() only after addWithUUID succeeds. If that is impossible because of the UUID clash, re-add the original dwarf on failure.
  Also reported as: Bounty reward item is consumed at action start, but the recipe is re-resolved at stop with no refund (`BountyRewardAction.java:237`).

- [x] **R39. Payment is destroyed when an in-flight transaction is rolled back**  
  Done 2026-10-23: `resetPaid()` moved from `start()` to the success path of `stop()` in Sign and Endorse, and onto the promoted copy in Promote, as ReputationGain already did. An interrupted action now keeps the coin or pouch charge.  
  `SignDwarfAction.java:27` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/action/type/profession/SignDwarfAction.java`  
  Sign, Promote and Endorse actions clear the dwarf's paid state in start(). The rollback (stopAction on death or when the revenge goal ends, plus reload after an unload) refunds the contract or tablet but does not restore paidTicks. So the player loses one coin, or one pouch charge, each time an action is interrupted.  
  Fix: Move resetPaid() from start() to stop(), i.e. only on successful completion, as ReputationGainDwarfAction already does. Alternatively record the paid state in InterruptedAction and restore it in recoverInterruptedAction.

- [x] **R40. Actions keep a Player reference across ticks and write rewards to a possibly stale player**  
  Done 2026-10-23: `InspectDwarfAction.resolvePlayer()` re-fetches the player by UUID from the player list (null when offline, removed, dead or in another level). Endorse, ReputationGain and BountyReward use it in `stop()` and roll back through `DwarfActionHelper.rollback` instead of rewarding a stale instance. The recovery refund drops at the dwarf when the player is dead instead of adding to an inventory that is about to be discarded.  
  `EndorseDwarfAction.java:59` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/action/type/reputation/EndorseDwarfAction.java`  
  Delayed inspect actions (Endorse, ReputationGain, BountyReward) apply their rewards in stop() to the Player object captured at start, with no online or removed check. If the player logs out on a multiplayer server while the dwarf's chunk keeps ticking, or respawns in under 2 s, the reward is written to a discarded ServerPlayer and lost. The tablet still drops showing the updated count. Singleplayer logout and a normal death-then-respawn are handled correctly by the interrupted-action refund and copyOnDeath.  
  Fix: Store only the UUID. In stop(), re-resolve the player with server.getPlayerList().getPlayer(uuid). If the player is absent, removed, dead or in another level, route through the same refund path as recoverInterruptedAction instead of granting rewards.

- [ ] **R41. DwarfRevengeGoal holds MOVE/LOOK for non-player attackers and its stop() cancels any in-progress action**  
  `DwarfRevengeGoal.java:52` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/ai/goal/dwarf/DwarfRevengeGoal.java`  
  DwarfRevengeGoal.stop() calls stopAction unconditionally. After the goal's own player hit it sets lastHurtByMob to null, so on the next tick the goal stops and that call cuts the ATTACK animation it just started after about 1 of its 10 ticks. The same call also aborts and refunds any inspect transaction running at the time. The idle MOVE/LOOK hold only happens for non-player attackers the priority-2 DwarfAttackGoal cannot path to, because for any mob it can reach that goal takes over.  
  Fix: Restrict canUse and canContinueToUse to Player attackers (or return false for non-players). Drop the stopAction call in stop(), or only stop when the active action type is ATTACK.

- [ ] **R42. Profession-typed dwarf entities created without finalizeSpawn keep profession NONE, so type and data disagree**  
  `AbstractDwarfEntity.java:195` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/base/AbstractDwarfEntity.java`  
  A profession-typed dwarf created without finalizeSpawn and without a 'profession' NBT tag (for example, /summon with any NBT argument) keeps PROFESSION=NONE: it gets NONE goals and attributes and no loadout, but still renders with the type-specific renderer, and NONE is then saved permanently. This is a mismatch between type and profession, not between client and server.  
  Fix: In readAdditionalSaveData add an else branch: setProfession(getSpawnProfession()) when the tag is missing. Alternatively define the PROFESSION default from getSpawnProfession() and apply attributes and goals when the level is a ServerLevel.

- [ ] **R43. Level-up fully rebuilds offers, resetting all uses and demand**  
  `AbstractTradingEntity.java:324` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/base/AbstractTradingEntity.java`  
  updateTradesForLevelUp calls rebuildTrades(FULL). It creates fresh offers for MAIN, exact-level and cumulative trades with uses=0 and rerolls the pools, and keeps only the GLOBAL offers.  
  Fix: On level-up, keep the existing non-global offers, copying their uses. Add only the offers whose recipe level equals the new level and the extra pool rolls. This matches the comment's intent for GLOBAL offers.

- [ ] **R44. Client returns SUCCESS for every dwarf interaction, so off-hand items can never be used**  
  `DwarfInteractions.java:120` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/interaction/DwarfInteractions.java`  
  DwarfInteractions.dispatch (line 129) returns SUCCESS for every client-side call. The client consumes the main-hand click and never sends an off-hand interact, so items held only in the off hand (coin, contract, bounty, crate and so on) are never processed by the server pipeline.  
  Fix: On the client, predict the result (for example, SUCCESS only when the held stack is relevant or the hand is empty and the dwarf can trade, otherwise PASS). Alternatively, return PASS from the Default and fallthrough path so the off hand is tried.

- [ ] **R45. Guildmaster career level-up runs before any gate**  
  `GuildmasterInteractionHandler.java:771` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/interaction/handler/profession/GuildmasterInteractionHandler.java`  
  GuildmasterInteractionHandler.preCore (lines 25-60) runs before the core gate pipeline (DwarfInteractions.java:153-157). When the player's reputation tier + 1 is above the guildmaster's merchant level, the guildmaster levels up, its trades are rebuilt and the 'yes' sound plays even when the Ignore, Language, Reputation or Busy/trading gate then rejects the interaction. Most of the harm is cosmetic, apart from offers being rebuilt while another player is trading.  
  Fix: Move the guildmaster level sync to postCore (after the gate handlers) or to the TradeInteractionHandler path. Alternatively, call it only when canTrade and the gates have passed.

- [ ] **R46. canRestock() is never overridden, so the screen's restock hint can never show**  
  `DwarfMerchant.java:44` · logic · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfMerchant.java`  
  AbstractTradingEntity never overrides the interface default, so every offers packet carries canRestock=false.  
  Fix: Override canRestock() in AbstractTradingEntity with a real condition, for example shouldRestock() or any offer being out of stock. Or remove the flag and the dead tooltip branch.

- [ ] **R47. Delirium curse is inert when the effect has infinite duration**  
  `DeliriumCurseEffect.java:82` · logic · `src/main/java/net/sievert/jolcraft/world/entity/effect/custom/harmful/curse/DeliriumCurseEffect.java`  
  An infinite-duration Delirium Curse reports getDuration() == -1, so the `<= 1` expiry check at DeliriumCurseEffect.java:82 runs cleanup on every tick and no episode ever fires. It is only reachable through commands, other mods, or corruption brewing with an input that is already infinite.  
  Fix: Use effect == null || (!effect.isInfiniteDuration() && effect.getDuration() <= 1).

- [ ] **R48. Unknown profession id in trigger JSON silently decodes to NONE instead of failing**  
  `DwarfEndorsementTrigger.java:69` · logic · `src/main/java/net/sievert/jolcraft/world/entity/player/advancement/custom/DwarfEndorsementTrigger.java`  
  The profession string codecs in DwarfEndorsementTrigger and DwarfTradeTrigger never reject an unknown profession id. DwarfProfession.byId falls back to NONE, so the null check is dead code. A misspelled id in a datapack advancement silently becomes NONE: the endorsement criterion can then never fire, and the trade criterion matches only NONE. Bundled datagen advancements always pass valid enum constants, so only hand-written or third-party datapacks are affected.  
  Fix: Use `JolCraftEnumHelper.byStringIdNullable(DwarfProfession.class, id, null)`, or reject `p == DwarfProfession.NONE`, in both trigger codecs (the same pattern is at DwarfTradeTrigger.java:62-66). JolCraftTriggerCodecs.strictStringMapped is already unused for this.

- [ ] **R49. Tooltip component built and discarded; line never shown (RewardCrateItem, SalvageItem)**  
  `RewardCrateItem.java:181`, `RewardCrateItem.java:161` · logic · `src/main/java/net/sievert/jolcraft/world/item/custom/crate/RewardCrateItem.java`  
  appendHoverText creates Component.translatable(...).withStyle(GRAY) but never adds it to the tooltip list. TOOLTIP_REWARD_CRATE and TOOLTIP_SALVAGEABLE are therefore never displayed.  
  Fix: Wrap each in tooltip.add(...) in both RewardCrateItem (line 161) and SalvageItem (line 27).
  Also reported as: Reward crate tooltip line is built and discarded (`RewardCrateItem.java:181`).

- [ ] **R50. Bounty task parchment is consumed but nothing is returned when objective resolution yields no single stack**  
  `BountyTaskRecipe.java:221` · logic · `src/main/java/net/sievert/jolcraft/world/recipe/custom/bounty/BountyTaskRecipe.java`  
  If a datapack or third-party bounty_task recipe has an objective pool that can produce zero or several stacks (because of conditions, several entries or extra rolls), or an entity count that can be 0 or less, createBounty returns EMPTY and BountyTaskAction.stop returns silently. The parchment, which was already consumed with CONSUME_ONE, is lost without any refund or message. This cannot happen with the mod's own shipped recipes.  
  Fix: Re-roll or fall back to another objective in createBounty. Alternatively, resolve the objective in BountyInteractionHandler before the item is consumed and deny the interaction if it is empty. At minimum, refund the input item in stop() when plannedResult is empty.

### Persistence and sync

- [ ] **R51. BreedingEntity reads its duplicate 'age' key with a default of 0, which turns babies into adults when the key is absent**  
  `AbstractBreedingEntity.java:109` · persistence · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/base/AbstractBreedingEntity.java`  
  AbstractBreedingEntity.readAdditionalSaveData (lines 109-110) reads its own lowercase 'age'/'forced_age' keys without a contains() guard, after vanilla has loaded 'Age'/'ForcedAge'. As a result, NBT given through /summon, /data merge or external tools that sets only the vanilla keys is ignored or reset to 0, and a summoned baby loads as an adult.  
  Fix: Remove the duplicate age keys and rely on AgeableMob's, or guard with compound.contains(NBT_AGE) / contains(NBT_FORCED_AGE) before overriding.

- [ ] **R52. Dwarf offer cost_b is read leniently, so a missing item silently drops the second cost**  
  `DwarfMerchantOffer.java:81` · persistence · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfMerchantOffer.java`  
  cost_b is decoded leniently, the same way vanilla decodes MerchantOffer buyB. If its item or component no longer exists (for example, after a mod is removed), the offer loads with only cost_a until the trades are rebuilt. Making the field strict without also decoding each list element separately would instead drop every offer on that dwarf.  
  Fix: Use optionalFieldOf so an invalid cost_b fails the offer, and drop only that offer (see next finding).

- [ ] **R53. Armor-set effect ownership flags live outside PlayerPersisted and are lost on player clone**  
  `JolCraftOwnedEffectHelper.java:133` · persistence · `src/main/java/net/sievert/jolcraft/world/entity/effect/JolCraftOwnedEffectHelper.java`  
  The ownership flags from JolCraftOwnedEffectHelper sit at the root of getPersistentData(), and ServerPlayer.restoreFrom drops them while copying the effects on an End-credits clone. The armor-set effects (Deepslate and Mithril) and the luminance Glowing effect then become unowned infinite effects that stay after the armor is removed or luminance ends, until milk or death.  
  Fix: Nest the ownership compound under Player.PERSISTED_NBT_TAG (PlayerPersisted), or recognise owned effects structurally (infinite duration plus exact amplifier) instead of using a flag.

### Registration

- [ ] **R54. Dwarven brew bucket is not in any creative tab**  
  `JolCraftCreativeModeTabs.java:98` · registration · `src/main/java/net/sievert/jolcraft/world/item/creative/JolCraftCreativeModeTabs.java`  
  DWARVEN_BREW_BUCKET is missing from the JolCraft creative tab: the brew section at JolCraftCreativeModeTabs.java:268-274 adds only the glass mug and the fresh brew mug. The bucket can still be obtained by filling a bucket from a brew cauldron or fluid.  
  Fix: Add output.accept(JolCraftItems.DWARVEN_BREW_BUCKET) next to the brew mug entry, if the omission is unintentional.

### Thread safety

- [ ] **R55. JEI trade recipe creation reads the integrated server's ServerPlayer and ServerLevel from the client thread, and the result differs between singleplayer and multiplayer**  
  `JeiDwarfTradeRecipe.java:571` · concurrency · `src/main/java/net/sievert/jolcraft/integration/jei/custom/dwarf_trade/JeiDwarfTradeRecipe.java`  
  The code path is real, but neither the claimed impact nor the medium severity holds up. resolveJeiLootContext (JeiDwarfTradeRecipe.java:571-610) is called for every trade recipe (line 147) and reads the integrated server's PlayerList and ServerPlayer from the client thread. However, only providers that are neither ConstantValue nor UniformGenerator ever use the context (lines 465-470, 493, 519). Every shipped trade cost is constant or uniform with constant bounds (374 uniform providers and nothing else in the generated dwarf_trade recipes). So the difference between singleplayer and a server only appears with third-party data packs. When it does, singleplayer shows a single fixed-seed sample as if it were an exact count, and a server shows "unknown". The cross-thread read is a single HashMap lookup by UUID. Its risk is negligible.  
  Fix: Do not build a loot context in JEI. Show a range for ConstantValue and UniformGenerator only, and "varies" for other providers, the same way on both sides. Remove the singleplayer-server access.

- [ ] **R56. Client logout clears the shared server-side config singletons in single player**  
  `ClientboundConfigSyncPacket.java:84`, `JolCraftClientDataEvents.java:19` · concurrency · `src/main/java/net/sievert/jolcraft/network/packet/s2c/ClientboundConfigSyncPacket.java`  
  clear() resets the same JVM singletons the integrated server reads. It runs from ClientPlayerNetworkEvent.LoggingOut on the client thread, while the integrated server thread may still be ticking and saving.  
  Fix: In the LoggingOut handler, skip the clear when the client is hosting the integrated server (Minecraft.getInstance().hasSingleplayerServer() or event.getConnection().isMemoryConnection()). Alternatively, clear on a dedicated client-only copy.
  Also reported as: Client logout resets server-shared config singletons in single player (`JolCraftClientDataEvents.java:19`).

### Performance

- [ ] **R57. Per-tick work on every LivingEntity (mobs included) allocates and walks attributes and effects**  
  `JolCraftEntityAttributeEventsHelper.java:64`, `JolCraftOwnedEffectHelper.java:119` · performance · `src/main/java/net/sievert/jolcraft/event/game/world/entity/attribute/JolCraftEntityAttributeEventsHelper.java`  
  tickLuminance calls syncInfinite on every server-ticked LivingEntity without a guard, and its isOwned/clearOwned calls use getPersistentData(). That creates the NeoForgeData compound once per entity, so every mob saves an empty NeoForgeData tag. The rest of the per-tick work is a handful of cheap attribute and effect map lookups, not repeated allocation. A cheap early return (no glowing effect and luminance <= 0 means skip) would avoid both.  
  Fix: Throttle the check, for example if ((entity.tickCount + entity.getId()) % 5 != 0) return; or react to effect and attribute changes through MobEffectEvent.Added/Remove and the existing onAttributeUpdated hook. In isOwned, avoid getPersistentData() when nothing is owned: add a cheap early exit (active==false && current==null returns before isOwned), or store ownership in an attachment instead of persistent data.
  Also reported as: Luminance owned-effect sync allocates persistent NBT on every living entity every tick (`JolCraftOwnedEffectHelper.java:119`).

- [ ] **R58. onAttributeUpdated mixin materialises and syncs an OVERHEAL attachment on every LivingEntity**  
  `LivingEntityMixin.java:75` · performance · `src/main/java/net/sievert/jolcraft/mixin/LivingEntityMixin.java`  
  On every LivingEntity's first tick, the MAX_HEALTH dirty flag set in its constructor makes jolcraft$clampOverheal call getData(OVERHEAL). This creates the attachment with value 0, sends one sync packet to players watching the entity, and adds a 'jolcraft:overheal' 0.0 entry to its saved NBT. The cost is one-time and small: one float plus one packet per entity.  
  Fix: At the top of the handler, return if entity.getExistingDataOrNull(JolCraftAttachments.OVERHEAL) is null (or !entity.hasData(...)), and skip when level().isClientSide. Only clamp entities that already hold overheal.

- [ ] **R59. Aging barrel copies the FluidStack (with data components) and recomputes age every tick**  
  `FermentingBarrelBlockEntity.java:289` · performance · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/brewing/FermentingBarrelBlockEntity.java`  
  For every non-empty barrel holding finished brew that can still age, tickServer calls getCurrentBrew(), which copies the stored FluidStack and applies addAgeInPlace on the copy. It does this every tick just to test canAgeFurther, then discards the copy.  
  Fix: Do the cheap gate first: if (level.getGameTime() % AMBIENT_SOUND_INTERVAL != 0L) return; and compute getCurrentBrew()/applyElapsedAge at that interval, or compute the remaining ticks until the next age threshold once and store it.

- [ ] **R60. Full discovered-structure set is re-synced to the client on every addition but nothing client-side reads it**  
  `JolCraftAttachments.java:62` · performance · `src/main/java/net/sievert/jolcraft/world/entity/attachment/JolCraftAttachments.java`  
  DISCOVERED_STRUCTURES (JolCraftAttachments.java:71, not :62) is registered with the one-argument .sync(codec). On every discovery, login and tracking start, NeoForge sends the whole uncapped GlobalPos list to the owner and to every player tracking the owner. No client code reads it. That wastes bandwidth and leaks a player's discovered structure coordinates to nearby clients. Remove .sync(...), or if a client reader is planned, use sync((holder, to) -> holder == to, codec) with a size-capped list.  
  Fix: Remove `.sync(...)` for DISCOVERED_STRUCTURES if the client never reads it, or add a size-limited list codec if a client reader is planned.

- [ ] **R61. Offer cost serializes the item's full default component map**  
  `DwarfTrades.java:214` · performance · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfTrades.java`  
  createItemCost builds the DataComponentPredicate with allOf(representative.getComponents()). That map includes every prototype default component, not only the recipe's required components.  
  Fix: Pass only the patch: DataComponentPredicate.allOf(DataComponentMap.builder() ...) built from representative.getComponentsPatch() additions. Or use DataComponentPredicate.EMPTY when the ingredient already enforces components.

- [ ] **R62. Full recipe-manager scan and holder re-wrap on every offer refresh**  
  `DwarfTrades.java:1042` · performance · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/trade/DwarfTrades.java`  
  getAllTradeRecipes iterates RecipeManager.getRecipes() (all recipes) and type-filters instead of using the type-indexed getAllRecipesFor(type). Only matching DwarfTradeRecipes are re-wrapped and copied. It runs 1-2 times per dwarf per restock interval (default 6000 ticks), which is a minor, avoidable cost and not a tick-spike risk.  
  Fix: Use recipeManager.byType(type) / getAllRecipesFor(type), which is indexed by type, and cast. Drop the RecipeHolder re-wrap and the List.copyOf. Pass the list through restock() and rebuild instead of re-fetching it.

- [ ] **R63. Armor set effect sync runs redundantly: once per armor piece in the inventory plus once per player tick, with string allocations**  
  `ArmorSetItem.java:72` · performance · `src/main/java/net/sievert/jolcraft/world/item/custom/armor/ArmorSetItem.java`  
  ArmorSetItem.inventoryTick (ArmorSetItem.java:64-81) repeats the work maintainSetEffects already does every player tick (HearthEquipmentEvents.java:19), once per carried set piece, with a small String allocation per effect. It is cleanup-level redundancy with negligible cost: delete the override and cache the id.  
  Fix: Delete the inventoryTick override. maintainSetEffects already covers every case, including pieces leaving the inventory, as its own comment says. Cache the id string in the record (compute once in the constructor) and make the sets a static final array. Optionally run maintainSetEffects every 10 ticks, or only on armor-slot change events.

- [ ] **R64. Corruption brewing recipe rebuilds the full output and rolls RNG on every brewing-stand tick**  
  `JolCraftCorruptionBrewingRecipe.java:85` · performance · `src/main/java/net/sievert/jolcraft/world/recipe/custom/vanilla/JolCraftCorruptionBrewingRecipe.java`  
  BrewingStandBlockEntity.serverTick calls isBrewable on every tick, which goes through hasMix and hasOutput to getOutput. JolCraftCorruptionBrewingRecipe.getOutput then does a full weighted roll and builds a discarded output stack. Because isBrewable stops at the first matching slot, this is usually about one build per stand per tick, not one per bottle slot.  
  Fix: This is a hot-path cost, not a correctness bug. Either accept it for now, or cache a cheap pre-check (hasEligibleEffect and the input checks) so the full roll only happens when the brew actually completes. A separate pure-predicate path would be needed, because IBrewingRecipe has no such hook.

### Resources, tags and ids

- [ ] **R65. Block tag shears_loot is declared but never generated or used**  
  `JolCraftTags.java:89` · resource-consistency · `src/main/java/net/sievert/jolcraft/data/JolCraftTags.java`  
  JolCraftTags.Blocks.SHEARS_LOOT (id built in JolCraftTagIds.SHEARS_LOOT) has no generated json and no code reference.  
  Fix: Delete the constant and the id, or populate it in JolCraftBlockTagProvider.
  Also reported as: Block tag SHEARS_LOOT is declared and given a translation but never populated or used (`JolCraftTags.java:89`).

- [ ] **R66. Legendary ancient tome base model uses the generic tome texture; ancient_dwarven_tome_legendary.png is orphaned**  
  `DwarfModelSubProvider.java:164` · resource-consistency · `src/main/java/net/sievert/jolcraft/datagen/client/model/subprovider/DwarfModelSubProvider.java`  
  generateLegendaryTomeModels hardcodes layer0 to item/book/tome/ancient_dwarven_tome for the base model. The shipped texture item/book/tome/ancient_dwarven_tome_legendary.png is referenced by no model, so it is dead.  
  Fix: If the base legendary art was intended, point layer0 at item/book/tome/ancient_dwarven_tome_legendary. Otherwise delete the orphan png.

- [ ] **R67. Pestles are in durability_enchantable but not in c:tools, and the c:tools children include no mod melee or armor items**  
  `JolCraftItemTagProvider.java:150` · resource-consistency · `src/main/java/net/sievert/jolcraft/datagen/tag/provider/JolCraftItemTagProvider.java`  
  Pestles (PestleItem extends ToolItem) are in minecraft:durability_enchantable but are missing from the c:tools builder at JolCraftItemTagProvider.java:150-153, while the other three custom tool tags are in it. The claim that the c:tools children include no mod melee or armor items is wrong: mod swords and warhammers reach c:tools through minecraft:swords, and armor does not belong in c:tools.  
  Fix: Add .addTags(JolCraftTags.Items.PESTLES) to the Tags.Items.TOOLS builder.

- [ ] **R68. lastTradedPlayer keeps a strong reference to a Player and is never read**  
  `AbstractTradingEntity.java:84` · resource-consistency · `src/main/java/net/sievert/jolcraft/world/entity/custom/dwarf/base/AbstractTradingEntity.java`  
  rewardTradeXp stores the ServerPlayer in a public field that is never cleared or used anywhere else.  
  Fix: Remove the field, or store a UUID or WeakReference.

- [ ] **R69. JolCraftProgressRenderer.clearTextureCache is never called, so cached NativeImages go stale after a resource reload**  
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

- [ ] **R76. Generator debug log reports 10 server providers but 14 are registered**  
  `JolCraftDataGenerator.java:199` · quality · `src/main/java/net/sievert/jolcraft/datagen/JolCraftDataGenerator.java`  
  The 'Server providers' debug line lists only 10 names, while 14 server providers are added. The damage-type, entity-type, instrument and advancement providers are not listed.  
  Fix: Build the log from a list of the providers that were actually added, or add the missing names.

- [ ] **R77. Duplicate COOKED_PORKCHOP entry doubles its weight in chest/supplies**  
  `JolCraftChestLootTableProvider.java:255` · quality · `src/main/java/net/sievert/jolcraft/datagen/loot/table/subprovider/JolCraftChestLootTableProvider.java`  
  Items.COOKED_PORKCHOP is added twice with weight 3 (lines 252 and 255), so its effective weight is 6 against 3 for every other cooked meat. This is almost certainly a copy-paste error.  
  Fix: Remove the duplicate, or replace it with the intended item (for example COOKED_RABBIT).

- [ ] **R78. Builder state is not reset or validated consistently, and there is dead code**  
  `DwarfTradeRecipeBuilder.java:937` · quality · `src/main/java/net/sievert/jolcraft/datagen/recipe/builder/DwarfTradeRecipeBuilder.java`  
  result(ItemLike,min,max) builds a LootPool local that is never used, and shadows the `pool` field. Several setters leave stale naming metadata when called twice.  
  Fix: Delete the unused local. Reset the name and coin flags at the start of every costA, costB and result setter, or compute them lazily at build time.

- [ ] **R79. Every _left/_right shaped recipe pair is a duplicate, because vanilla shaped recipes already match mirrored patterns**  
  `ToolRecipesSubProvider.java:292` · quality · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/ToolRecipesSubProvider.java`  
  The tool recipes and misc lockpick, glass_mug and compass-dial recipes register an explicit mirror-image pattern as a second recipe. ShapedRecipePattern matches mirrored layouts automatically, so each pair is two recipes with identical inputs and outputs.  
  Fix: Emit a single pattern per tool. Asymmetric patterns are mirror-matched by vanilla already.

- [ ] **R80. Inconsistent miner task recipe ids (bounty_novice vs crate_*)**  
  `DwarfMinerBountyTasks.java:219` · quality · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/bounty/task/DwarfMinerBountyTasks.java`  
  Miner task recipes get the ids bounty_novice and crate_<tier>, while every other profession uses plain <tier> ids. The cause is a duplicate emitBountyTier/emitTier pair that differs only in the id prefix and sound2. This is cosmetic: nothing looks the ids up, and the sounds have no shared convention across professions anyway.  
  Fix: Use a single emitTier that outputs id = tier name and one consistent sound pair, as the other three professions do.

- [ ] **R81. Stone Skin degradation guard in the duration helper can never match (dead code)**  
  `JolCraftEffectDurationEventsHelper.java:33` · quality · `src/main/java/net/sievert/jolcraft/event/game/world/entity/effect/util/JolCraftEffectDurationEventsHelper.java`  
  isStoneSkinDegradation never matches the case it was written for, because Stone Skin's level loss goes through forceAddEffect, which posts no MobEffectEvent.Added. It can only match an unrelated addEffect of Stone Skin one level lower with the same duration, and then it would wrongly skip Focus. JolCraftEffectEvents also keeps four empty damage hooks.  
  Fix: Remove the guard, or switch the degradation to addEffect and keep it. Delete the empty hook methods.

- [ ] **R82. Dead code: unused compassExamples helper**  
  `JeiHandInteractionHelper.java:198` · quality · `src/main/java/net/sievert/jolcraft/integration/jei/custom/hand_interaction/JeiHandInteractionHelper.java`  
  The private helper compassExamples(...) at JeiHandInteractionHelper.java:198 is never called, because the JEI builder makes its examples from group.structures(). Datagen builds the structure tag from that same list, so the examples only differ from what the compass really targets if a datapack overrides the tag.  
  Fix: Delete the method, or use it in place of the group.structures() stream if the tag is the source of truth.

- [ ] **R83. AbstractSyncingBlockEntity is dead code with a write-only hook**  
  `AbstractSyncingBlockEntity.java:11` · quality · `src/main/java/net/sievert/jolcraft/world/block/entity/custom/base/AbstractSyncingBlockEntity.java`  
  AbstractSyncingBlockEntity has no subclasses, so it is dead code, and Barrel and Cauldron repeat its boilerplate. The client is not left without data: NeoForge's default handleUpdateTag and onDataPacket pass the update tag to loadWithComponents/loadAdditional, so no paired load hook is needed.  
  Fix: Delete the class, or make Barrel and Cauldron extend it and add a paired loadClientData hook.

- [ ] **R84. Dead no-op call in normalizeBrewingFluid**  
  `DwarvenBrewFluidHelper.java:346` · quality · `src/main/java/net/sievert/jolcraft/world/block/fluid/util/brewing/DwarvenBrewFluidHelper.java`  
  normalizeBrewingFluid (void) makes a copy at line 283 and then throws away the result of isSameFluidSameComponents at lines 346-349, so both are dead code. The callers in FermentingCauldronBlockEntity:709 and FermentingBarrelBlockEntity:539 already make their own pre-normalization copies, so no change-tracking is lost.  
  Fix: Remove the copy and the call. Alternatively, return the comparison result as a boolean 'changed' if callers need it.

- [ ] **R85. Intermediate un-generated keys are registered in JolCraftLootTables.LOCATIONS**  
  `JolCraftLootTables.java:161`, `JolCraftLootTables.java:196` · quality · `src/main/java/net/sievert/jolcraft/world/loot/JolCraftLootTables.java`  
  Every constant wrapped with chest/strongbox/crate(key)/archaeology/fishing also registers its bare key in LOCATIONS (the themed crate overload does not). So all() would return keys that have no generated table. Nothing calls all() today, so this is latent.  
  Fix: Build the foldered key directly, without registering the intermediate one, and register only the final key.
  Also reported as: JolCraftLootTables registers phantom un-foldered ids and its all() set is wrong (`JolCraftLootTables.java:196`).

- [ ] **R86. Recipe hook ids are not validated at load time**  
  `JolCraftRecipeHooks.java:38` · quality · `src/main/java/net/sievert/jolcraft/world/recipe/base/output/hook/JolCraftRecipeHooks.java`  
  An unknown hook ResourceLocation in a recipe's hooks list passes codec and ValidationContext checks (ItemOutput.validate and RecipeOutput.validate never look at hooks), and only logs an error at execution, returning false.  
  Fix: In ItemOutput, EntityOutput, SoundOutput and EffectOutput validate(), call context.reportProblem for any hook id not in JolCraftRecipeHooks.HOOKS (expose a contains() method), and make the codecs fail with it.

- [ ] **R87. DwarvenFortressStructure ignores its serialized start_height, so the generated config is misleading**  
  `DwarvenFortressStructure.java:98` · quality · `src/main/java/net/sievert/jolcraft/world/worldgen/structure/custom/DwarvenFortressStructure.java`  
  startPos() is overridden to derive Y from the stone column, but the codec still requires a start_height field and the datagen writes absolute 0. The value is dead data.  
  Fix: Either make start_height optional in AbstractJigsawStructure.codec, or apply it as an offset in spawnOrigin. Document that it is unused.

### Found while fixing (added after the review)

- [ ] **R88. corruption_effects.json flaps between datagen runs**  
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

1. **Lang overhaul** — related: R65, R49
2. **Tag cleanup (use common)** — related: R1, R65, R67, R14, R21
3. **Datagen overhaul** — related: R72, R77, R79, R80, R73, R19, R78, R66, R85, R24, R87, R18, R20
4. **Datagen logs even for normal startup** — related: R76
5. **Sound overhaul** — related: R10, R15
6. **Advancements** — related: R48
7. **Config** — related: R6, R56, R60
8. **Capabilities** — related: R32, R59, R83
9. **Commands** — related: R42, R9, R51
10. **Gametests** — related: R75, R71, R70, R72
11. **Integration** — related: R55, R82, R69

## Larger design items from the review (not single fixes)

- Strongbox menu lifecycle: wire startOpen/stopOpen/removed(), drop the per-tick recheck, clear the interacting-player id (R12, R15, R34, R33).
- Dwarf transactions as refund-safe units: resolve before consuming, store player UUIDs, move resetPaid() to success (R39, R40, R38). Done 2026-10-23.
- One fail-soft policy for data-driven config and recipe validation (R6, R86, R48, R17).
- Obtainability and economy pass (balance, after the 1.0 content): Muffhorn spawn, Blacksmith/Smelter/Arcanist/Priest progression, Artisan vs Merchant price overlap, bounty arbitrage (R2, R23, R22).
- Event-handler preamble: isCanceled, getUseBlock/getUseItem, mayUseItemAt, spectator and sneak checks for every RightClickBlock and BreakEvent handler (R25, R26, R28, R31).
- Per-tick early-outs: skip getPersistentData/getData unless the entity has the relevant effect or attachment (R57, R58, R63).
