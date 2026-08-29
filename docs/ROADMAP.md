# Roadmap

AgenticKitchen remains a pantry-first cooking orchestrator rather than a generic recipe feed or social meal-planning app.

Core product loop:

**What is in my kitchen? → What can I cook? → Choose the best option → Build a safe timed plan → Coordinate cooking → Update what was used.**

Status terms remain strict: implementation, automated verification, and physical-device verification are separate claims.

## Phase 0 — managed AI foundation

Current priority before new pantry/product expansion.

- Default managed AI path through Firebase AI Logic for users who should not need an API key.
- Firebase App Check with debug provider locally and Play Integrity for release.
- Direct Gemini BYOK remains an optional advanced provider.
- Deterministic offline provider remains available and is the managed-provider fallback when Firebase is unavailable.
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

- Generate missing-item lists from the selected recipe and actual pantry state.
- Never add ingredients already sufficiently stocked.
- Group by practical shopping category such as Produce, Meat, Dairy, Pantry, Other.
- One action to add recipe shortages to shopping.
- Offer substitution before purchase where appropriate.
- Shopping completion can later feed confirmed items into pantry inventory.

## Phase 5 — multi-photo kitchen scan

Use vision only where visual inference adds value.

Capture flow can include multiple labelled views:

- Fridge
- Freezer
- Pantry
- Counter

AI produces structured candidates with confidence and uncertainty. A review screen must allow add/remove/edit/location correction. Inventory never changes until explicit user confirmation.

## Phase 6 — recipe import

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

1. Close managed Firebase AI automated and exact-head physical verification.
2. Smart Pantry expiry + locations.
3. Deterministic recipe matching/ranking.
4. Recipe Options UI.
5. Pantry-aware substitutions.
6. Smart Shopping.
7. Home UI refinement.
8. Multi-photo scan.
9. Recipe import.
10. My Recipes.
11. Recipe Detail refinement.
12. Cooking Mode polish and notifications.
13. Receipt scan.
14. Meal planner.
15. Advanced UX.

Every major slice must preserve regression coverage for cooking scheduling, pantry reservations/consumption, allergies/safety, offline fallback, and existing physically accepted behaviour. Old-SHA physical evidence never proves a newer source SHA.
