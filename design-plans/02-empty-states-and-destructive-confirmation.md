# Add Actionable Empty States and Confirmation Dialogs for Destructive Actions

Written against: 0c999b9

## Evidence chain

- Surface: `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt` and `android/app/src/main/java/com/fridge/manager/ui/screens/FamilyAndSettingsScreen.kt`
- Problem:
  1. When `filteredItems.isEmpty()` (`InventoryScreen.kt:849-875`), the empty state renders only an emoji and text with no next action button.
  2. Deleting a storage location (`InventoryScreen.kt:620-622`) and logging out (`FamilyAndSettingsScreen.kt:207-213`) trigger immediately without a confirmation `AlertDialog`.
  3. Discarding an item from `FreshItemCard` or `FoodItemDetailSheetContent` should also be protected by a confirmation dialog if triggered directly.
- Design evidence: `android/DESIGN.md` (Components: Empty States & Destructive Actions) and `baseline-ui` rules (`MUST give empty states one clear next action`, `MUST use an AlertDialog for destructive or irreversible actions`).
- Owner: `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`
- Scope and affected surfaces: `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`
- Uncertainty: none

## Design decision

Upgrade empty states to provide a single context-aware CTA ("필터 초기화" when filters/search are active, or "첫 품목 등록하기" when the household has no items) and gate destructive actions (`onDeleteLocation`, `onDiscardItem`, `onLogout`) behind Material 3 `AlertDialog` confirmations.

## Reuse

- Material 3 `AlertDialog`, `Button`, `TextButton` (already used in `InventoryScreen.kt:941-967`)
- `FreshKitchenColors.PrimaryTealDeep`, `FreshKitchenColors.ExpiredText`
- Exemplar: `showAddLocationDialog` (`InventoryScreen.kt:939-968`)

## Changes

1. `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt`
   - Change:
     - In the `filteredItems.isEmpty()` branch (`lines 849-875`), distinguish between `items.isEmpty()` ("아직 등록된 식재료가 없어요" + `품목 등록하기` button opening `showAddItemSheet`) and active filter mismatch ("조건에 맞는 식재료가 없어요" + `필터 초기화` button resetting location/category/expiry/search filters).
     - Add `deleteTargetLocation: StorageLocationNode?` state and show a confirmation `AlertDialog` before invoking `onDeleteLocation(loc.id)` from the tree editor (`line 620`).
     - Add `discardTargetItem: FoodItem?` state and show a confirmation `AlertDialog` before invoking `onDiscardItem(item.id)` from card or detail sheet discard buttons.
   - Preserve: Existing filter reset logic, `showAddItemSheet` trigger, and `onDeleteLocation`/`onDiscardItem` signatures.
   - Verify: Empty states always display one clear CTA; tapping delete/discard opens a confirmation dialog before mutating state.

2. `android/app/src/main/java/com/fridge/manager/ui/screens/FamilyAndSettingsScreen.kt`
   - Change:
     - Add `showLogoutConfirm` state (`remember { mutableStateOf(false) }`).
     - Trigger an `AlertDialog` ("로그아웃할까요?", explaining local cache will be cleared) when tapping `로그아웃` (`line 207`), invoking `onLogout()` only on confirm.
   - Preserve: `onLogout` callback and profile header layout.
   - Verify: Accidental taps on `로그아웃` do not immediately sign out the user.

## Scope

- Inherit: Inventory list empty states, location tree deletion, item discard, family tab logout
- Verify: `InventoryScreen`, `FamilyAndSettingsScreen`
- Exclude: ViewModel/Repository business logic (callbacks remain unchanged)

## Validation

- Product: Test empty household, filtered-to-zero search, location deletion cancel/confirm, item discard cancel/confirm, and logout cancel/confirm.
- Interface: Confirm dialogs use `FreshKitchenColors.ExpiredText` for destructive confirm buttons and standard `TextButton` for cancel.
- System: No new parallel dialog components introduced; uses standard Material 3 `AlertDialog`.
- Repository: `./gradlew testDebugUnitTest assembleDebug` (in `android/`) → exits 0.

## Stop conditions

- Stop if `onDiscardItem` already has a nested confirmation flow inside `FoodItemDetailSheetContent` that would double-prompt (verify before wiring).

## Design documentation

- After acceptance and validation: Update `docs/04-guides/05-ui-design-guidelines.md` and `docs/01-planning/02-roadmap.md` Phase 5.3 checklist.
