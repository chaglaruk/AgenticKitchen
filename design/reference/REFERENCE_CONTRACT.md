# AgenticKitchen UI Overhaul Reference Contract

Status: APPROVED for implementation.

## Approved visual systems

- A — Modern Minimal
- B — Premium Dark
- K — Luxe Appliance Dark
- L — Warm Editorial Utility
- M — Minimal Pro Control

These are five structurally distinct presentation systems, not five palette swaps. Shared business logic, state, navigation semantics and callbacks must remain common. Theme-specific Compose compositions are allowed where required to reproduce the approved visual references.

## Appearance behavior

`FOLLOW_SYSTEM` is the default preference.

- Android system Light -> A — Modern Minimal
- Android system Dark -> K — Luxe Appliance Dark
- A/B/K/L/M are all manually selectable in Settings > Appearance.
- A manual selection remains fixed regardless of system light/dark until the user returns to `FOLLOW_SYSTEM`.
- Theme changes must apply without requiring a process restart.
- System/status/navigation bar icon brightness must follow the resolved visual system.

## Authority order

When references disagree, use this order:

1. Current runtime feature/state behavior and current repository source.
2. Approved golden visual references from `AgenticKitchen_5Theme_Reference.zip`.
3. Full-coverage structural references and theme specs from that archive.
4. Implementation convenience.

Mockup text, counts, ingredient quantities and recipe names are sample data unless the current app state supplies them. Never hard-code mockup data.

## Full reference archive

Canonical archive filename: `AgenticKitchen_5Theme_Reference.zip`

SHA-256: `6e6a853eaff0546f01df170b040a4645ed0b45a0713f3456a408615eca1fefb8`

The canonical archive contains 25 approved golden references, 180 full-coverage structural references, manifest/theme/feature-parity/screen-mapping documents, per-theme specs and the original UI audit mapping.

The archive must be stored under `design/reference/archive/` using Git LFS before production UI implementation proceeds. Do not commit the 53 MB archive as a normal Git blob.

## Feature parity is mandatory

The redesign must retain every current user-facing capability documented by the current source and supplied UI audit, including manual/scan/import/library ingredient capture, persistent pantry and freshness, recipe generation/matching/import, recipe detail, plan review, substitutions, safety, AI Sous-Chef, pan check, active timers and step controls, completion/consumption deduction, history reuse, hardware/dietary/language/provider/settings controls, and all audited loading/error/empty/dialog/camera/assistant states.

Do not silently simplify or remove capabilities because a mockup does not visibly show them.

## Verification standard

A screen is not complete because it compiles or looks broadly similar.

For each implementation checkpoint:

1. Render the requested visual system/state on the target emulator viewport.
2. Capture screenshots from the exact implementation commit.
3. Compare against the corresponding reference using side-by-side review, 50% overlay and image diff where practical.
4. Correct geometry, spacing, typography, image crop, surface hierarchy, CTA placement, navigation height, safe-area behavior and state visibility.
5. Verify callbacks and feature parity against current source.
6. Run project automated verification.
7. Run `$reviewer` and `$scope-guard` before declaring the checkpoint complete.

## Implementation sequencing

Do not attempt all 36 states x 5 systems in one uncontrolled rewrite.

First stabilize:

1. five-theme runtime architecture and preference migration
2. Settings > Appearance
3. Home/Kitchen family in all five systems
4. emulator screenshot comparison

Then continue by surface family after the foundation is proven.

## Existing project invariants

PR #1 remains open, draft and unmerged. Stay on `refactor/agentic-kitchen-production-foundation`. Do not create another branch or PR. Do not merge, ready-for-review, rebase, amend, squash, reset or force-push. Preserve provider separation and do not weaken existing security/AI behavior as part of UI work.
