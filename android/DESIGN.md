---
version: alpha
name: Fresh Kitchen Material
description: Design system for the Refrigerator Manager (우리집 냉장고) Android Jetpack Compose app.
colors:
  primary: "#00685F"
  primary-dark: "#005049"
  secondary: "#0D9488"
  primary-soft: "#CCFBF1"
  primary-mint: "#9CF2E8"
  background: "#F8FAFC"
  surface: "#FFFFFF"
  surface-muted: "#EFF4F8"
  border-subtle: "#E2E8F0"
  text-primary: "#0B1C30"
  text-secondary: "#3D4947"
  text-muted: "#64748B"
  expired-bg: "#FEE2E2"
  expired-text: "#DC2626"
  today-bg: "#FFEDD5"
  today-text: "#EA580C"
  expiring-bg: "#FEF3C7"
  expiring-text: "#D97706"
  safe-bg: "#DCFCE7"
  safe-text: "#16A34A"
  none-bg: "#F1F5F9"
  none-text: "#64748B"
typography:
  sans:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
  headline:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 24px
    lineHeight: 32px
    fontWeight: 700
  title-large:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 20px
    lineHeight: 28px
    fontWeight: 700
  title-medium:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 16px
    lineHeight: 24px
    fontWeight: 700
  title-small:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 14px
    lineHeight: 20px
    fontWeight: 600
  body-medium:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 14px
    lineHeight: 20px
    fontWeight: 400
  body-small:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 12px
    lineHeight: 18px
    fontWeight: 400
  label-large:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 14px
    lineHeight: 20px
    fontWeight: 600
  label-medium:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 12px
    lineHeight: 16px
    fontWeight: 600
  label-small:
    fontFamily: Pretendard, Noto Sans KR, sans-serif
    fontSize: 11px
    lineHeight: 16px
    fontWeight: 600
rounded:
  card: 16px
  control: 12px
  pill: 9999px
spacing:
  screen-margin: 16px
  card-padding: 16px
  section-gap: 12px
---

## Overview

우리집 냉장고 (Refrigerator Manager) is a family-shared Android application for managing hierarchical storage locations, food inventory quantities, and expiry D-Day alerts. Its interface uses a calm, high-legibility light surface built on Material Design 3 with a deep teal accent and semantic expiry status badges.

## Colors

- Use `{colors.primary}` for primary call-to-action buttons, active filter chips, floating action buttons, and owner badges; restrict strong accent fills to one focal action per view.
- Pair `{colors.background}` as the screen canvas with `{colors.surface}` cards bordered by `{colors.border-subtle}` and `{colors.surface-muted}` for recessed controls such as input fields, quantity steppers, and summary tiles.
- Reserve semantic expiry tokens (`expired-*`, `today-*`, `expiring-*`, `safe-*`, `none-*`) exclusively for D-Day badges, summary status counters, and remaining-quantity urgency indicators so urgency states remain immediately distinguishable.

## Typography

- Preserve the Material 3 type scale across all screens; never override a typography slot with an empty `TextStyle` that drops `fontSize` or `lineHeight`.
- Apply tabular figures (`fontFeatureSettings = "tnum"`) to all numeric readouts, including D-Day counters, inventory counts, quantity steppers, and notification timestamps.
- Truncate dense single-line titles, location breadcrumbs, and item memos with ellipsis to prevent vertical card jitter.

## Layout

- Maintain consistent horizontal screen margins using `{spacing.screen-margin}` across top bars, cards, and bottom sheets.
- Optimize for one-handed thumb-zone operation: keep bottom navigation limited to two primary tabs (`재고·장소`, `가족·알림`) and keep primary creation actions reachable via the floating action button or bottom sheets.
- Abbreviate storage location breadcrumbs deeper than four levels (`... > 냉동실 > 하단 서랍`) so paths stay readable on a single line.

## Shapes

- Apply `{rounded.card}` uniformly to all content cards and bottom-sheet interior panels.
- Apply `{rounded.control}` to buttons, text inputs, and inline steppers.
- Apply `{rounded.pill}` to filter chips, status pills, segmented tabs, and floating action buttons.

## Components

- **Food Item Cards**: Present the item thumbnail or category icon, item name, a single semantic D-Day badge, location breadcrumb, remaining-quantity progress bar, and compact inline quantity controls without competing visual accents.
- **Empty States**: Always include one clear next action (such as resetting active filters or opening the item registration sheet) alongside the empty-state message.
- **Destructive Actions**: Gate irreversible or high-impact actions—such as deleting a storage location, discarding an item, or logging out—behind a confirmation `AlertDialog`.

## Do's and Don'ts

- **Do** reuse named tokens from `FreshKitchenColors` and `MaterialTheme.typography` across all screens.
- **Do** show validation or submission errors adjacent to the triggering action.
- **Don't** use background gradients or glow effects on cards or onboarding surfaces.
- **Don't** introduce hardcoded hex color literals in screen composables when a semantic token exists.
- **Don't** stack multiple redundant warning badges on the same item card.
