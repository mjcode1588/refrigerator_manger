# Modernize Inventory Screen Header Density and Food Item Card Hierarchy

Written against: 0c999b9

## Evidence chain

- Surface: `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt` (Top header stack `lines 268-846` and `FreshItemCard` `lines 1244-1560`)
- Problem:
  1. Six separate vertical control rows stack before the first food item (`Top App Bar` → `Household Switcher + Tune` → `3 Summary Cards` → `Storage Location Header + Sub-location toggle + Chip Row` → `Search Bar` → `Sort/Category/Status/Reset Row`), pushing inventory items below the fold and creating visual clutter.
  2. `FreshItemCard` stacks a `3.5dp` top accent stripe, an extra `"확인필요"` pill next to `"D+N 만료"`, a remaining-quantity progress bar, and an action row on every card, causing heavy visual density.
  3. Two separate icons (`Tune` at `line 390` and `AccountTree` at `line 533`) toggle the exact same `showTreeManager` state right next to each other.
- Design evidence: `android/DESIGN.md` (Layout & Components), `baseline-ui` rules (limit accent color usage per view, remove redundant visual clutter).
- Owner: `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt`
- Scope and affected surfaces: `InventoryScreen.kt`
- Uncertainty: Confirm whether the user prefers keeping the inline `-`/`+` quantity stepper directly on `FreshItemCard` or moving secondary actions (`이동`/`폐기`) strictly into the detail bottom sheet.

## Design decision

Streamline the top chrome of `InventoryScreen` from 6 stacked rows down to 4 calm, high-scannability bands (unified Top Bar with household switcher + tree manager action, compact 3-column summary strip, clean search + horizontal filter/location bar) and simplify `FreshItemCard` by removing duplicate warning badges, removing the top accent stripe in favor of clean border/badge hierarchy, and giving card content breathing room.

## Reuse

- `FreshKitchenColors`, `FreshKitchenShapes`, `FreshTypography` (`Theme.kt`)
- `SummaryStatCard`, `LocationPillChip`, `resolveExpiryBadgeStyle` (`InventoryScreen.kt`)

## Changes

1. `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt` (Top header consolidation)
   - Change:
     - Merge the household switcher pill (`lines 323-377`) directly into the Top App Bar title slot (replacing the redundant static `"우리집 냉장고"` + separate household row below it), keeping the single `장소 관리` icon button and profile avatar on the trailing side.
     - Remove the duplicate `AccountTree` button in `LazyRow` (`lines 524-540`) since `장소 관리` is already in the top bar.
     - Keep `SummaryStatCard` compact (`vertical` padding `10.dp`, muted neutral elevation, subtle border only when selected) so the first food cards are visible immediately on load.
   - Preserve: Multi-household switching dropdown, `includeSubLocations` toggle, search bar, sort/category/status/reset filters.
   - Verify: Vertical height above the first `FreshItemCard` is reduced by at least ~80–100dp and no duplicate tree-manager button exists.

2. `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt` (`FreshItemCard` visual polish)
   - Change:
     - Remove the `3.5.dp` `topAccentColor` bar (`lines 1285-1292`) and the redundant `"확인필요"` pill (`lines 1341-1354`); let the single semantic D-Day badge (`D+N 만료` / `D-Day` / `D-N`) carry expiry urgency cleanly.
     - Use subtle flat card styling (`16.dp` radius, `1.dp` `BorderSubtle`, `elevation = 0.dp` or `1.dp`) with `16.dp` inner padding and clearer spacing between title/meta, remaining-quantity bar, and quick actions.
     - Show the remaining-quantity progress bar cleanly (keeping `tnum` alignment) and ensure quick actions (`-`/`+` stepper and `소비`/`이동` buttons) have adequate touch targets (`>= 36.dp`) without crowding.
   - Preserve: Card tap → `FoodItemDetailSheetContent`, inline `-`/`+` quantity stepper, thumbnail WebP image with category emoji fallback.
   - Verify: Expired, expiring-soon, and safe cards each display exactly one urgency badge and read cleanly at a glance.

## Scope

- Inherit: Main inventory tab (`InventoryScreen`)
- Verify: Single-household vs multi-household header, long household names, long item names, 0 / 1 / many items
- Exclude: `AddItemSheetContent`, `FoodItemDetailSheetContent`, `AuthOnboardingScreen`, `FamilyAndSettingsScreen`

## Validation

- Product: Switch households from the top bar, toggle tree editor, filter by location/category/status, and adjust item quantity inline.
- Interface: Verify on standard mobile viewport (360dp–412dp width) that summary strip + location chips + first 2–3 food cards fit cleanly without text clipping.
- System: Confirm `FreshKitchenShapes` and `FreshKitchenColors` are used exclusively without hardcoded hex values.
- Repository: `./gradlew testDebugUnitTest assembleDebug` (in `android/`) → exits 0.

## Stop conditions

- Stop if removing the `"오늘의 살림 팁"` banner or changing quick-action buttons on `FreshItemCard` conflicts with user preference (confirm first).

## Design documentation

- After acceptance and validation: Update `docs/04-guides/05-ui-design-guidelines.md` (Section 3.3 layout rules and changelog) and `docs/01-planning/02-roadmap.md` Phase 5.3 checklist.
