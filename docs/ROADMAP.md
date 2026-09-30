# Roadmap

AgenticKitchen remains a pantry-first cooking orchestrator rather than a generic recipe feed or social meal-planning app.

Core product loop:

**What is in my kitchen? → What can I cook? → Choose the best option → Build a safe timed plan → Coordinate cooking → Update what was used.**

Status terms remain strict: implementation, automated verification, and physical-device verification are separate claims.

## Phase 0 — managed AI foundation / Production AI Access

**Architecture audit: COMPLETE. Production hardening remains a parallel pre-release workstream and does not block My Recipes feature development.**

Target ordinary-user UX: **install → grant required permission → take/import a photo → managed AI works without entering a Gemini/Firebase API key.**

Current production-access direction:

- Firebase AI Logic is the normal/default managed path.
- Provider/model/API-key choice must not be part of ordinary onboarding.
- Direct Gemini BYOK remains available only as an Advanced / power-user option.
- Offline remains a supported secondary path; automatic fallback must be capability-aware and must never bypass safety/validation failures.
- Release builds already install Play Integrity App Check; console-side enforcement, API restrictions, conservative quotas, and budget protection are release gates.
- Model names remain remotely managed through Firebase Remote Config.
- Recipe-import vision and cooking-vision routing should be separable so a safe recipe-photo fallback is not coupled to cooking-photo model choice.
- Prepare an application-level entitlement/access-policy seam for future Free/Pro limits without adding subscription infrastructure yet.
- Do not add Firebase Auth, Firestore, Analytics, cloud sync, Storage, or unrelated backend infrastructure unless a concrete product/security requirement appears.

Existing foundation:

- Default managed AI path through Firebase AI Logic for users who should not need an API key.
- Firebase App Check with debug provider locally and Play Integrity for release.
- Direct Gemini BYOK remains an optional advanced provider.
- Deterministic offline provider remains available as an explicit secondary provider; capability-aware automatic fallback is planned but not yet claimed as implemented.
- Keystore-backed BYOK credential storage and plaintext migration.
- SDK-enforced structured JSON schemas plus application decode/validation.
- Task-aware model routing:
  - extraction/parsing → lower-cost Flash-Lite class;
  - recipe/cooking reasoning → Flash class;
  - cooking-photo judgement → Flash class.
- Firebase Remote Config controls non-secret model names so models can be changed without an APK release.
- Conservative Firebase/Gemini quotas and later application-level usage metering.
- No Firebase Auth, Firestore, Analytics, cloud sync, Storage, or unrelated backend infrastructure.
- Exact-head physical smoke is required before managed Firebase behaviour is labelled VERIFIED.

## Phase 1 — Smart Pantry 2.0

Extend the existing SQLDelight pantry instead of replacing it.

- Expiry/use-by or best-before metadata.
- Inventory states: Fresh, Use Soon, Expires Today, Expired, Low Stock.
- Locations: Fridge, Freezer, Pantry, Counter, Other/custom.
- Sorting by expiry, name, and quantity.
- Fast actions: Used, Ran Out, Edit Quantity, Move, Add to Shopping.
- Home-level **Use first** strip for ingredients that should be consumed soon.
- Keep the compact inventory presentation; add detail only where useful.

## Phase 2 — deterministic recipe matching and ranking

Do not spend AI tokens on comparisons that local data can answer.

Recipe-result groups:

- Ready Now
- Missing 1
- Missing 2
- AI Ideas

Ranking priorities:

1. allergy and food-safety constraints;
2. pantry coverage;
3. ingredients expiring soon;
4. number/importance of missing ingredients;
5. requested ready time;
6. equipment compatibility;
7. dietary preferences;
8. previous successful recipes;
9. user preference/history.

Recipe cards should surface pantry coverage, duration, servings, and whether they consume expiring ingredients.

## Phase 3 — pantry-aware substitutions

Substitution must be a structured plan mutation, not merely chat text.

When an ingredient is unavailable:

- suggest only plausible pantry-aware alternatives;
- show the user what will change;
- update ingredient quantities;
- regenerate/revalidate relevant cooking instructions and timing;
- re-run equipment/resource/dependency validation;
- update reservation/consumption planning;
- keep allergy and safety validation fail-closed.

## Phase 4 — Smart Shopping

**Implementation checkpoint: AUTOMATED_ONLY.** Persistent smart shopping, deterministic shortage amounts, category grouping, recipe-shortage reconciliation, and substitution-aware updates are implemented. The gated source workflow passed shared tests, Android unit tests, lint, and debug assembly before committing. Physical-device QA is intentionally not claimed at this checkpoint.

- Generate missing-item lists from the selected recipe and actual pantry state.
- Never add ingredients already sufficiently stocked.
- Group by practical shopping category such as Produce, Meat, Dairy, Pantry, Other.
- One action to add recipe shortages to shopping.
- Offer substitution before purchase where appropriate.
- Shopping completion can later feed confirmed items into pantry inventory.

## Phase 5 — multi-photo kitchen scan

**Implementation checkpoint: AUTOMATED_ONLY.** Labelled Fridge / Freezer / Pantry / Counter scanning, structured candidate review, confidence/uncertainty display, editing/removal/location correction, and explicit-confirm inventory mutation are implemented. The gated source workflow passed shared tests, Android unit tests, lint, and debug assembly before committing. Physical-device and real-camera/managed-vision QA remain separate and are not claimed here.

Use vision only where visual inference adds value.

Capture flow can include multiple labelled views:

- Fridge
- Freezer
- Pantry
- Counter

AI produces structured candidates with confidence and uncertainty. A review screen must allow add/remove/edit/location correction. Inventory never changes until explicit user confirmation.

## Phase 6 — recipe import

**Current checkpoint: RECIPE_IMPORT_PHYSICAL_VERIFIED — CLOSED.** Recipe photo uses the physically accepted `gemini-3.5-flash-lite + STRICT_SCHEMA` combination through Remote Config.

Android share/import entry points:

- URL
- plain text
- screenshot/photo
- Android share intent

Flow:

1. extract a structured recipe;
2. show an import summary;
3. compare against pantry;
4. show available/missing/substitutable ingredients;
5. convert the imported recipe into the validated AgenticKitchen cooking-plan pipeline.

Text/known-format parsing should be deterministic where practical; AI is a fallback for ambiguous extraction rather than a mandatory hop.

## Phase 7 — My Recipes

**Current checkpoint: PHYSICAL CORE VERIFIED on v12 / `d1874e2`; acceptance-found domain fixes are pending exact-head delta verification.** Save/open/delete/persistence/navigation/session recovery/cook-count were physically verified. The same acceptance run exposed two real edge cases: saved AI recipes using count-style units such as `slice/dilim` could be blocked on re-prepare, and zero-quantity pantry rows could be misclassified as review-required instead of missing. The v13 fix keeps normal imports fail-closed for truly unknown units, recognizes `slice/dilim` as count units, lets already-saved recipes proceed to the normal shortage path when pantry measurement dimensions cannot be converted safely, and treats zero stock as valid non-negative pantry state. Do not advance this phase to CLOSED until the v13 delta is physically checked.

Unify useful recipes without turning the product into a content feed.

Sources can include:

- imported recipes;
- saved AI recipes;
- local/offline recipes;
- manually saved recipes;
- successfully cooked history items.

Prefer known/successful recipes before generating a new AI recipe when they satisfy the current pantry and constraints.

## Phase 8 — Home UI refinement

Maintain the existing AgenticKitchen editorial identity while borrowing proven information architecture from comparable products.

Primary actions:

- Cook With What I Have
- Scan My Kitchen

Secondary actions:

- Add Ingredient
- Import Recipe

Then, in restrained sections:

- Use First
- Cook Now
- compact pantry inventory

Do not turn Home into a dense dashboard.

UI references are principles, not visual copies:

- Pantry Pic: scan entry and clear primary actions;
- KitchenPal: inventory information architecture;
- Cooklist: expiry and pantry lifecycle;
- SuperCook: ingredient-first matching/filtering;
- SideChef: recipe-card hierarchy and food imagery;
- ReciMe: low-friction import flow;
- Pestle: guided-cooking usability;
- Samsung Food: planner structure.

All typography, spacing, colour, artwork, and components remain one AgenticKitchen design system.

## Phase 9 — Pantry UI refinement

- Location tabs such as Fridge / Freezer / Pantry.
- Expiry/name/quantity sort.
- Grid/list choice only if both modes prove useful.
- Compact card: artwork, name, quantity, minimal expiry/status badge.
- Detail: quantity, unit, location, added date, use-by date, status.
- Avoid KitchenPal-style control/icon overload.

## Phase 10 — Recipe Options UI refinement

- Result count and pantry-coverage summary.
- Segments: Ready / Missing 1 / Missing 2 / AI.
- Strong recipe image/artwork where available.
- Time, servings, pantry match, and use-soon indicator on the card.
- Preserve pantry-first decision support rather than creating an endless inspiration feed.

## Phase 11 — Recipe Detail / Prepare refinement

Surface before preparation:

- duration;
- servings;
- pantry coverage;
- equipment;
- ingredients already available;
- missing ingredients;
- substitutions;
- plan preview.

Primary action remains **Prepare Recipe**. Secondary actions include shopping, substitution, and save.

## Phase 12 — Cooking Mode polish

Build on the existing AgenticKitchen scheduler rather than replacing it with a conventional single-step recipe reader.

Preserve:

- dependency-aware schedule;
- parallel active operations;
- countdowns;
- pause/resume;
- complete/skip;
- persisted recovery.

UI emphasis:

- one large primary operation;
- compact simultaneous operations;
- next operations preview;
- progress;
- stable cooking controls;
- Assistant and Pan Check remain secondary to the active cooking task.

Later enhancements can include notification controls and hands-free interaction, but the deterministic cooking runtime stays authoritative.

## Phase 13 — receipt to pantry

- Scan a grocery receipt as a structured extraction task.
- Review every candidate before insertion/merge.
- Merge quantities into known inventory only after confirmation.
- Preserve uncertainty instead of inventing quantities/products.

## Phase 14 — meal planner

Keep the first planner deliberately small.

- weekly Mon–Sun view;
- assign recipes to days/meals;
- calculate pantry availability;
- consolidate missing shopping items;
- prefer ingredients expiring during the planning window;
- forecast expected pantry consumption where data is reliable.

Do not introduce household accounts or cloud sync merely to support planning.

## Phase 15 — advanced UX

Only after core flows are reliable:

- voice batch ingredient entry;
- cooking voice controls;
- hands-free cooking;
- timer/lock-screen notifications;
- craving intent;
- previous-plan reuse;
- personalized local ranking;
- optional Gemini Live experiments when the API is production-suitable for the required interaction.

## Monetization direction

Launch model: **useful Free tier + Pro subscription, no banner/interstitial advertising.**

Why:

- cooking is a high-attention workflow where interruptive ads damage usability and safety;
- AI cost scales with AI calls, while generic ad revenue scales with impressions, so the economics do not align;
- subscription entitlements can track expensive AI value much more directly.

Free should remain genuinely useful with unlimited/local core functionality such as pantry, expiry, deterministic matching, shopping, saved recipes, and offline cooking where practical. Managed AI gets a modest quota.

Pro can include a generous managed-AI allowance and premium AI-heavy workflows such as multi-photo scans, receipt extraction, high-volume imports, advanced substitutions, assistant/vision usage, and advanced planning.

Initial pricing hypothesis to validate with real usage/cost data:

- about £3.99/month;
- about £29.99/year, with annual as the primary offer.

Do not promise truly unlimited managed AI. Use fair-use/usage limits and conservative service quotas.

BYOK and Pro are separate concepts. A user bringing a Gemini key changes who pays the model bill; it does not automatically unlock paid AgenticKitchen product entitlements.

Possible later experiments, not launch requirements:

- one-time/lifetime Pro BYOK tier where no managed AI allowance is included;
- rewarded ad for an extra AI credit only after real scale proves it worthwhile.

For commercial release, introduce only the minimal Google Play purchase/entitlement verification backend needed for secure subscription validation. Do not expand it into general user accounts, analytics, pantry/recipe cloud sync, or unrelated infrastructure.

## Explicitly out of scope for now

- social feed;
- followers/community network;
- comments and public recipe marketplace;
- badges/gamification;
- calorie diary or weight tracking;
- grocery-retailer ordering integrations;
- household accounts/cloud sync;
- smart-appliance ecosystem integrations;
- restaurant/chef marketplace;
- broad Firebase backend services.

## Execution order

Current feature sequence after the closed Recipe Import gate:

1. **My Recipes runtime/UI/nav/save/open/delete/cook completion.**
2. **Receipt-specific import completion.**
3. **Meal Planner.**
4. **UI / themes / motion work.**
5. **Final regression.**

Parallel pre-release workstream: **Production AI Access**

1. Managed-default UX and Advanced-only BYOK/provider controls.
2. Split recipe-import vision routing from cooking-vision fallback where needed.
3. Capability-aware offline fallback policy without safety/validation bypass.
4. App Check production enforcement, API-key restrictions, conservative rate limits, and spend/budget protection.
5. Lightweight Free/Pro/BYOK access-policy abstraction; no entitlement backend until commercial launch requires it.
6. Authentication remains deferred unless a concrete abuse-control or entitlement requirement justifies it.

Every major slice must preserve regression coverage for cooking scheduling, pantry reservations/consumption, allergies/safety, offline fallback, and existing physically accepted behaviour. Old-SHA physical evidence never proves a newer source SHA.