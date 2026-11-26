# Restore Android Typography Scale and Unify Design Tokens

Written against: 0c999b9

## Evidence chain

- Surface: `android/app/src/main/java/com/fridge/manager/ui/theme/Theme.kt` and all screens consuming `FridgeTheme` (`AuthOnboardingScreen.kt`, `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`)
- Problem:
  1. `FreshTypography` replaces Material 3 `Typography` slots with bare `TextStyle(fontFeatureSettings = "tnum")`, wiping out default `fontSize`, `lineHeight`, `fontWeight`, and `letterSpacing` across the entire app.
  2. Card corner radii diverge across screens (`24.dp` in `AuthOnboardingScreen.kt`, `22.dp` in `FamilyAndSettingsScreen.kt`, `20.dp` in `InventoryScreen.kt`, `16.dp` in `SummaryStatCard`) against the documented `16dp` rule.
  3. `FreshKitchenColors.ExpiringBg` is `0xFFFEF7CD` instead of the documented `#FEF3C7`, hardcoded color literals (`0xFFFACC15`, `0xFFE0F7F4`, `0xFF6EE7B7`, `0xFFE2E8F0`, `0xFFCBD5E1`) appear directly in screen files, and `AuthOnboardingScreen.kt` applies a `Brush.linearGradient` to the household creation card.
- Design evidence: `docs/04-guides/05-ui-design-guidelines.md` (Section 3.1–3.3), `android/DESIGN.md`, `baseline-ui` rules (no gradients, use existing theme tokens).
- Owner: `android/app/src/main/java/com/fridge/manager/ui/theme/Theme.kt`
- Scope and affected surfaces: `Theme.kt`, `AuthOnboardingScreen.kt`, `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`
- Uncertainty: none

## Design decision

Restore full Material 3 typography metrics in `FreshTypography` by copying from `Typography()` and merging `fontFeatureSettings = "tnum"` plus explicit line heights, then centralize card/control corner radii (`16.dp` card, `12.dp` control, `999.dp` pill) and semantic color tokens in `Theme.kt` while removing hardcoded hex literals and card gradients.

## Reuse

- `FreshKitchenColors` (`Theme.kt`)
- `FridgeTheme` (`Theme.kt`)
- Exemplar: `SummaryStatCard` (`InventoryScreen.kt:1097`) for `16.dp` card radius and `SoftInputField` (`AuthOnboardingScreen.kt`) for recessed input styling.

Introduce a shared `FreshKitchenShapes` object (or Material 3 `Shapes`) in `Theme.kt` and a shared `FridgeSurfaceCard` composable in `ui/components/FridgeComponents.kt` because `StitchCard` (`FamilyAndSettingsScreen.kt`), `FreshItemCard` (`InventoryScreen.kt`), and onboarding cards (`AuthOnboardingScreen.kt`) currently duplicate card elevation, border, and corner radius with drifting values.

## Changes

1. `android/app/src/main/java/com/fridge/manager/ui/theme/Theme.kt`
   - Change:
     - Define `FreshTypography` by starting from `val base = Typography()` and calling `.copy(fontFeatureSettings = "tnum", ...)` on `headlineMedium`, `titleLarge`, `titleMedium`, `titleSmall`, `bodyMedium`, `bodySmall`, `labelLarge`, `labelMedium`, `labelSmall` matching `android/DESIGN.md`.
     - Update `FreshKitchenColors.ExpiringBg` from `Color(0xFFFEF7CD)` to `Color(0xFFFEF3C7)`.
     - Add `FreshKitchenShapes` (`Card = RoundedCornerShape(16.dp)`, `Control = RoundedCornerShape(12.dp)`, `Pill = RoundedCornerShape(999.dp)`) and pass Material 3 `Shapes` into `FridgeTheme`.
     - Remove unused legacy `KakaoYellow` / `KakaoText` tokens if unreferenced.
   - Preserve: `FreshKitchenColors` primary teal palette (`#00685F`, `#0D9488`, `#CCFBF1`, `#9CF2E8`) and D-Day semantic badge contract.
   - Verify: Headings, card titles, body copy, and D-Day badges render with distinct size/weight hierarchy and aligned tabular numerals.

2. `android/app/src/main/java/com/fridge/manager/ui/screens/AuthOnboardingScreen.kt`
   - Change:
     - Replace `RoundedCornerShape(24.dp)` on login/onboarding cards with `FreshKitchenShapes.Card` (`16.dp`) and `RoundedCornerShape(14.dp)` on buttons/inputs with `FreshKitchenShapes.Control` (`12.dp`).
     - Remove `Brush.linearGradient(...)` on the "새 우리집 만들기" card (`lines 527-534`) and use flat `FreshKitchenColors.SurfaceWhite` with `1.dp` `FreshKitchenColors.BorderSubtle`.
     - Replace hardcoded `Color(0xFF6EE7B7)` and `Color(0xFFE2E8F0)` with `FreshKitchenColors` tokens.
   - Preserve: Login/register segmented tab flow, household creation toggle, invite code paste button, and Debug demo pill.
   - Verify: Onboarding cards have crisp, flat `16.dp` surfaces with no gradients or hardcoded colors.

3. `android/app/src/main/java/com/fridge/manager/ui/screens/FamilyAndSettingsScreen.kt` and `InventoryScreen.kt`
   - Change:
     - Standardize card corner radius (`22.dp` / `20.dp` → `16.dp` via `FreshKitchenShapes.Card`) and replace hardcoded `Color(0xFFFACC15)`, `Color(0xFFE0F7F4)`, `Color(0xFFCBD5E1)` with `FreshKitchenColors` tokens.
   - Preserve: All interactive callbacks, bottom sheet drag handles, and D-Day badge semantics.
   - Verify: Zero stray `Color(0xFF...)` literals remain in screen files except category metadata definitions if kept intentional.

## Scope

- Inherit: All Compose screens wrapped in `FridgeTheme`
- Verify: `AuthOnboardingScreen`, `InventoryScreen`, `FamilyAndSettingsScreen`, `MainActivity` bottom bar
- Exclude: Backend API, Room entities, WorkManager logic, `web-proto/`, `front/`

## Validation

- Product: Open login, inventory list, item detail sheet, and family settings tabs; verify clear visual hierarchy and consistent card geometry.
- Interface: Check Korean text wrapping, long item names, and numeric alignment across `D-Day`, `남은 양`, and stepper counts.
- System: Confirm `FreshTypography` preserves non-unspecified `fontSize` and `lineHeight` on every style and no card gradients remain.
- Repository: `./gradlew testDebugUnitTest assembleDebug` (in `android/`) → exits 0 with all tests passing.

## Stop conditions

- Stop if any custom font resource import is requested (wait for user confirmation before adding `.ttf` assets).

## Design documentation

- After acceptance and validation: Record the typography fix, token unification, and `android/DESIGN.md` link in `docs/04-guides/05-ui-design-guidelines.md` and mark the corresponding item in `docs/01-planning/02-roadmap.md`.
