# Add Natural Screen Transitions and Micro-Interactions

Written against: 0c999b9

## Evidence chain

- Surface: `MainActivity.kt`, `AuthOnboardingScreen.kt`, `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`
- Problem:
  1. Switching between auth/onboarding and main tabs (`MainActivity.kt:93, 181`) uses raw `if/else` branches with abrupt frame cuts.
  2. Switching between `로그인` and `가족 등록` tabs (`AuthOnboardingScreen.kt:224`), toggling the storage tree editor (`InventoryScreen.kt:569`), and toggling local notifications (`FamilyAndSettingsScreen.kt:485`) snap in/out abruptly.
  3. Filtering, sorting, or removing food items in `LazyColumn` (`InventoryScreen.kt:877`) teleports cards without placement animation, and adjusting quantity jumps the remaining-quantity progress bar (`InventoryScreen.kt:1473`) instantaneously.
- Design evidence: Explicit user request for natural screen transitions and motion, governed by `baseline-ui` motion constraints (`ease-out` entrance, interaction feedback `<= 200ms`, compositor-friendly `opacity`/`translation` motion, no gratuitous looping or full-screen blur animations).
- Owner: `MainActivity.kt`, `AuthOnboardingScreen.kt`, `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`
- Scope and affected surfaces: All 4 Android UI entry/screen files
- Uncertainty: none

## Design decision

Introduce restrained, fast (`140ms–200ms`, `FastOutSlowInEasing` / `LinearOutSlowInEasing`) Compose transitions across top-level navigation, tab/segment switches, collapsible panels, `LazyColumn` item reordering, and progress/chip feedback so every state change feels responsive and continuous without sluggishness.

## Reuse

- Compose Animation APIs (`AnimatedContent`, `AnimatedVisibility`, `Crossfade`, `animateFloatAsState`, `animateColorAsState`, `Modifier.animateItem()`) — already bundled with Compose Material 3 / Foundation, no new dependencies required.
- Centralize timing constants (`MotionDurationFast = 150`, `MotionDurationStandard = 200`) in `Theme.kt` (`FreshKitchenMotion`) so all screens share one consistent rhythm.

## Changes

1. `android/app/src/main/java/com/fridge/manager/ui/theme/Theme.kt`
   - Change: Add `FreshKitchenMotion` object with `FastMs = 150`, `StandardMs = 200`, and `EnterEasing = FastOutSlowInEasing`.
   - Preserve: Existing theme structure.
   - Verify: All screen animations reference `FreshKitchenMotion` constants.

2. `android/app/src/main/java/com/fridge/manager/MainActivity.kt`
   - Change:
     - Wrap Auth ↔ Main root switch (`line 93`) in `Crossfade(animationSpec = tween(FreshKitchenMotion.StandardMs))`.
     - Wrap bottom tab content (`selectedTab == 0` ↔ `1`, `line 181`) in `AnimatedContent` using directional subtle horizontal slide (`±24.dp` / `width / 16`) + `fadeIn(tween(180))` together with `fadeOut(tween(140))`.
   - Preserve: Bottom bar state, snackbar host, `LaunchedEffect` refresh behavior.
   - Verify: Switching between `재고·장소` and `가족·알림` glides smoothly without flash or jank.

3. `android/app/src/main/java/com/fridge/manager/ui/screens/AuthOnboardingScreen.kt`
   - Change:
     - Wrap `로그인` / `가족 등록` form body (`line 224`) in `AnimatedContent` with subtle directional slide + fade (`180ms`) and `SizeTransform(clip = false)`.
     - Animate `AuthSegmentTab` container/text color with `animateColorAsState(tween(150))`.
   - Preserve: All form state variables (`username`, `password`, `nickname`, `registrationCode`).
   - Verify: Segment tab switching feels smooth and preserves entered values.

4. `android/app/src/main/java/com/fridge/manager/ui/screens/InventoryScreen.kt` and `FamilyAndSettingsScreen.kt`
   - Change:
     - Add `Modifier.animateItem()` on `FreshItemCard` inside `LazyColumn` (`InventoryScreen.kt:877`) for smooth filter/sort/removal reordering.
     - Animate `remainingRatio` in `FreshItemCard` (`line 1435`) with `animateFloatAsState(tween(200, easing = FastOutSlowInEasing))`.
     - Wrap `showTreeManager` panel (`InventoryScreen.kt:569`) and notification settings body (`FamilyAndSettingsScreen.kt:485`) in `AnimatedVisibility(enter = fadeIn(tween(180)) + expandVertically(tween(200)), exit = fadeOut(tween(140)) + shrinkVertically(tween(180)))`.
     - Animate `LocationPillChip` and `SummaryStatCard` background/border transitions with `animateColorAsState(tween(150))`.
   - Preserve: All click handlers, filter logic, and scroll performance.
   - Verify: Filtering, sorting, quantity `-`/`+`, and panel toggles animate cleanly within `200ms` with no frame drops.

## Scope

- Inherit: Root auth/main switch, bottom tabs, auth segment tabs, inventory list items, progress bars, collapsible panels
- Verify: `MainActivity`, `AuthOnboardingScreen`, `InventoryScreen`, `FamilyAndSettingsScreen`
- Exclude: Heavy custom spring physics, shared-element photo hero transitions (can be added later if needed), backend/data layers

## Validation

- Product: Switch tabs, toggle login/register, filter/sort items, tap `-`/`+` quantity, toggle tree editor and notification switch.
- Interface: Confirm every transition completes within `200ms`, respects directional cues (left/right tab order), and does not clip shadows or inputs.
- System: No new Gradle dependencies added; `FreshKitchenMotion` owns shared durations.
- Repository: `./gradlew testDebugUnitTest assembleDebug` (in `android/`) → exits 0.

## Stop conditions

- Stop if `Modifier.animateItem()` is unavailable in the current Compose BOM version (fall back to `animateItemPlacement()` or check Compose Foundation version in `libs.versions.toml`).

## Design documentation

- After acceptance and validation: Record motion timing standards in `docs/04-guides/05-ui-design-guidelines.md` and mark Plan 04 complete in `docs/01-planning/02-roadmap.md`.
