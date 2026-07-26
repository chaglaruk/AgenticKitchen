Agentic Kitchen — Mimari Özeti

- Android-first, Kotlin Multiplatform (KMM) shared core.
- Shared modülde ajanlar: `Orchestrator`, `TimingAgent`, `IngredientAgent`, `VisionAgent` (interface'ler).
- Veri: SQLDelight (öneri) + JSON seed verisi.
- UI (Android): Jetpack Compose minimal giriş (Ne var? + Saat kaçta hazır olsun?).
- Zamanlama: reverse scheduling (target time -> back-calc), resource reservation, delta reschedule on vision feedback.

Bu dosya proje başlangıcı için referans; detaylar `/memories/session/plan.md` içinde tutuluyor.
