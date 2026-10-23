# Review todo

Working list from the repository review of 2026-10-17. Numbers are stable: refer to an item as **R12**. Tick items off when fixed, or mark them `won't fix` with a reason; fixed items are removed from this file on cleanup (last cleanup 2026-10-23, the fixes are in the `Fixes` and `Review fix R..` commits). Line numbers are from the `Trades` branch on 2026-10-17 and will drift.

Status: every review item is fixed or decided (R64 is won't fix: accepted hot-path cost). 2 balance items are parked below. Everything compiles; `runData` and `runGameTestServer` pass.

Working order: bugs first, then the 1.0 content, then balance. Content and balance findings are not review items.

## Parked: balance (after the 1.0 content)

Working order agreed 2026-10-23: fix every bug first, then implement the 1.0 content, then balance. These two are balance findings, not defects, so they wait for that pass. R8 (Blacksmith/Smelter levelling) was removed the same day as content work, not a review item.

- [ ] **R22. Merchant bounty items are mostly sold by the merchant for no more than the reward (arbitrage)**  
  `DwarfMerchantBountyTasks.java:60` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/bounty/task/DwarfMerchantBountyTasks.java`  
  Most NOVICE and APPRENTICE merchant bounty items are sold by the merchant in the same amounts for less than the expected coin reward. A player can buy the item, fill the bounty and redeem it for about +2-3 coins plus dwarf and player XP each time. The loop is not unlimited. Redeeming restocks only the bounty offer, while the item trades have 3 uses and come from a random pool. It continues only until those trades run out or a restock crate drops.  
  Fix: Draw task items from things the merchant does not sell, or scale rewards so that expected reward is below the merchant's sell price of the task items.

- [ ] **R23. Artisan buy prices and Merchant sell prices overlap, so coins can be farmed by choosing favourable rolls**  
  `DwarfArtisanTrades.java:62` · logic · `src/main/java/net/sievert/jolcraft/datagen/recipe/subprovider/dwarf_trade/DwarfArtisanTrades.java`  
  The per-unit price ranges overlap between Merchant buy offers and Artisan sell offers for quartz, lapis, emerald and amethyst, so with favourable independent rolls a player can turn a profit. The Merchant's 3 uses per restock cap it, so it is a minor balance problem and not a medium logic defect.  
  Fix: Make Artisan's maximum coins-per-unit strictly lower than the Merchant's minimum coins-per-unit for every shared item, for example by lowering Artisan's coin rewards or raising the Merchant's coin cost.

## Notes left from the fixes

Choices made while fixing that are worth a second look; the full reasoning is in the commits.

- R74: `LICENSE` says "Copyright (c) 2025-2026 Dr.Sievert"; adjust the holder or years if wanted.
- R77: the duplicate `COOKED_PORKCHOP` in `chest/supplies` became `COOKED_RABBIT`, the one cooked meat the pool lacked; swap for a plain removal if rabbit was not intended.
- R80: the miner novice task's second sound became the cartographer sound, matching the other four tiers.
- R88: `loot_table/chest/supplies.json` can still reorder `neoforge:cures` between `runData` runs, because the brew effects go through vanilla's `PotionContents` codec. The mod-owned codecs sort the list; a generator-level sort belongs to the datagen overhaul (roadmap 3).

## Roadmap (author's list)

The author's own planned work, in the author's order. Every review item that touched these areas is fixed; the notes above are what is left to fold in.

1. **Lang overhaul**
2. **Tag cleanup (use common)**
3. **Datagen overhaul** — fold in the R88 residual (a generator-level sort of `neoforge:cures`)
4. **Datagen logs even for normal startup**
5. **Sound overhaul**
6. **Advancements**
7. **Config**
8. **Capabilities**
9. **Commands**
10. **Gametests**
11. **Integration**

## Larger design items from the review (not single fixes)

- Obtainability and economy pass (balance, after the 1.0 content): Muffhorn spawn, Blacksmith/Smelter/Arcanist/Priest progression, Artisan vs Merchant price overlap, bounty arbitrage (R22, R23).
