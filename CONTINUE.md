# DEVAM / HANDOFF — Agentic Kitchen

Bu doküman, projeyi devralan herhangi bir yapay zeka ajanının (AI Agent) veya geliştiricinin, uygulamanın amacını, felsefesini, mimarisini, kullanılan teknolojileri, dosya yapısını, kritik kod bloklarını ve tarihçesini tam olarak anlaması için hazırlanmış kapsamlı bir rehberdir. **Bu dosyayı okuyan bir ajan, projeyi doğrudan build edebilmeli ve geliştirebilmelidir.**

---

## 0. 2026-04-24 Snapshot / Nerede Kalındı
- **Navigation split:** Uygulama artık 4 tab'li bir akış kullanıyor: `Intelligence`, `Options`, `Operations`, `Configuration`.
- **SQLDelight persistence active:** Pişirme geçmişi artık `agentic.db` içinde saklanıyor. `HistoryRepository` üzerinden erişim sağlanıyor.
- **AI Refresh & Back support:** `OptionsScreen` içinde "Yenile" dendiğinde AI artık daha önce önerdiklerinden farklı tarifler üretiyor.
- **Hardware-Aware AI:** Tarif üretilirken kullanıcının ocağı (seviye bazlı), fırını ve Airfryer varlığı detaylı olarak prompt'a ekleniyor.
- **Modernized Media:** `CameraModal` içindeki deprecated `getBitmap` çağrıları `ImageDecoder` (API 28+) ile modernize edildi.
- **Snackbar Feedback:** API hataları ve kota sorunları için `UiEvent` tabanlı `Snackbar` bildirim sistemi eklendi.

---

## 1. Proje Amacı ve Felsefesi
**Agentic Kitchen**, mutfak deneyimi "sıfır" olan kullanıcılar odaklı, inisiyatifi tamamen yapay zekaya (algoritmaya) bırakan, **"Askeri netlikte operasyon planı"** sunan bir Akıllı Şef Uygulamasıdır. 
* **Sıfır İnisiyatif:** "Orta ateş", "göz kararı", "yüksek ısı" gibi yoruma açık, klasik yemek tarifi terimleri YASAKTIR. Uygulama kullanıcının ocağının kaç seviyeli olduğunu (örn: 1-9) bilir ve doğrudan "Ocağı 7. seviyeye al, 4 dakika kavur" emrini verir.
* **Katı Kurallar (Ingredient Agent):** Kullanıcı bir malzemeyi değiştirmek istediğinde, eğer lezzet profilini bozacaksa sistem kesin bir dille reddeder. Sadece kimyasal ve lezzet profili olarak uygunsa teknik bir alternatif sunar.
* **Görsel Kontrol (Vision Agent):** Kullanıcı doğradığı soğanın veya tavadaki etin fotoğrafını çeker. Uygulama "Soğanlar çok iri, daha ufalt" veya "Mantarlar hala çok sulu, ocağı 9'a alıp 2 dakika daha kavur" diyerek müdahale eder.

---

## 2. Teknoloji Yığını ve Mimari
* **Platform:** Android (Min SDK 24, Target SDK 34)
* **Dil:** Kotlin 1.9.10, Kotlin Multiplatform (shared module) altyapısına hazır.
* **UI Framework:** Jetpack Compose (Material 2) — `androidx.compose.material:material:1.5.4`
* **AI SDK:** `com.google.ai.client.generativeai:generativeai:0.6.0` — Gemini 1.5 Flash
* **Database:** SQLDelight 2.0.0 (SQLite)
* **Mimari:** Clean Architecture & MVVM (Model-View-ViewModel)
  * `AppViewModel`: Tüm StateFlow'ları (tema, donanım ayarları, tarif planları) yönetir.
  * `StateFlow`: UI bileşenleri state'leri observe ederek kendini re-render eder.
* **Kalıcılık (Persistence):** `SharedPreferences` (pref name: `"agentic_prefs"`)
* **Tema ve UI:** Özel `AppColors` sınıfı ile Dinamik Tema (Green, Blue, Orange, Dark) altyapısı mevcuttur.
* **Loglama:** `AppLogger` singleton — Logcat (TAG: `"AK"`) + cihaz dosyası (`files/agentic_log.txt`)
* **Build Sistemi:** Gradle 8.9, AGP 8.1.4, Kotlin Compiler Extension 1.5.3

---

## 3. Dosya Yapısı ve Roller

```
agentic-kitchen/
├── settings.gradle.kts              # Root project — includes :shared ve :app-android
├── build.gradle.kts                 # Root-level Gradle config
├── CONTINUE.md                      # ← Bu dosya (handoff rehberi)
│
├── shared/                          # KMP shared module (domain layer)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/agentickitchen/shared/
│       ├── agents/
│       │   ├── IngredientAgent.kt           # Interface
│       │   ├── IngredientAgentImpl.kt       # SimpleIngredientAgent — substitution mantığı
│       │   ├── TimingAgent.kt               # Interface
│       │   ├── SimpleTimingAgent.kt         # Zamanlama hesaplama
│       │   ├── Orchestrator.kt              # Interface
│       │   ├── OrchestratorImpl.kt          # SimpleOrchestrator — RecipeSession → ScheduleEvent
│       │   ├── VisionAgent.kt               # Interface (gelecek için)
│       │   └── TestRunner.kt                # Test yardımcısı
│       ├── models/
│       │   ├── HardwareProfile.kt           # HardwareProfile data class
│       │   ├── Ingredient.kt                # IngredientAmount data class
│       │   ├── RecipeSession.kt             # RecipeSession + RecipeStep data class
│       │   ├── ScheduleEvent.kt             # ScheduleEvent data class
│       │   ├── SubstitutionDecision.kt      # Malzeme değişim kararı
│       │   └── VisionCheckResponse.kt       # Görsel kontrol yanıtı
│       └── db/
│           └── HistoryRepository.kt         # SQLDelight sorgu yöneticisi
│       └── sqldelight/com/agentickitchen/shared/db/
│           └── AppDatabase.sq               # ★ Veritabanı şeması ve sorgular
│
├── app-android/                     # Android UI modülü
│   ├── build.gradle.kts             # AGP + Compose + Gemini dependency
│   ├── src/main/AndroidManifest.xml # CAMERA + INTERNET izinleri
│   └── src/main/java/com/agentickitchen/android/
│       ├── AppLogger.kt             # ★ Merkezi loglama sistemi
│       ├── AppViewModel.kt          # ★ Ana ViewModel — tüm state + AI orchestration
│       ├── HardwareProfileManager.kt# Donanım profil yöneticisi
│       ├── MainActivity.kt          # Activity + AppRoot + AppNavigation composable
│       ├── ui/
│       │   ├── Theme.kt             # AppColors, 4 palette, AgenticTheme, getBgGradient
│       │   ├── HomeScreen.kt        # Ana ekran — malzeme, kategori, plan görünümü
│       │   ├── SetupScreen.kt       # İlk kurulum — ekipman, porsiyon, saat seçimi
│       │   ├── SettingsScreen.kt    # Ayarlar — donanım dialog, diyet, tema, dil, API key
│       │   ├── CameraModal.kt       # Kamera/galeri → Bitmap → AI tarama
│       │   ├── ApiKeyOnboarding.kt  # ★ İlk açılışta API key isteme dialog'u
│       │   ├── CalibrationScreen.kt # Placeholder
│       │   └── PlanView.kt          # Placeholder
│       └── vision/                  # Gelecek görüntü işleme uzantıları
```

---

## 4. Temel Veri Yapıları ve Domain

### AppViewModel.kt İçindeki Modeller
```kotlin
// Kullanıcının donanımı — ocak gücü, fırın özellikleri, API key
data class HardwareSettings(
    val stoveType: String = "electric",  // "electric" | "gas"
    val stovePowerMax: Int = 9,          // Ocağın max seviyesi
    val ovenAvailable: Boolean = true,
    val ovenHasFan: Boolean = true,
    val ovenHasGrill: Boolean = false,
    val servingSize: Int = 2,
    val powerLevel: Int = 7,
    val geminiApiKey: String = ""        // Kullanıcının Gemini API Key'i
)

data class DietSettings(val dietType: String = "none", val allergies: Set<String> = emptySet())

// Tarif seçenekleri
data class RecipeOption(val id: String, val type: String, val name: String, val description: String)

// UI State Machine
sealed class PlanState {
    object Idle : PlanState()           // Hiçbir şey yapılmadı
    object Loading : PlanState()        // AI düşünüyor
    data class OptionsReady(...)        // 3 tarif seçeneği hazır
    data class RecipeActive(...)        // Kullanıcı bir tarifi seçti, adımlar aktif
    data class Error(val message: String) : PlanState()
}
```

### Shared Module Modelleri (RecipeSession.kt, ScheduleEvent.kt)
```kotlin
data class RecipeStep(
    val id: String, val type: String, val device: String,
    val durationSec: Int, val dependsOn: List<String> = emptyList(),
    val instruction: String = ""
)
data class RecipeSession(
    val sessionId: String, val targetTimeIso: String,
    val ingredients: List<IngredientAmount>,
    val hardwareProfileId: String, val steps: List<RecipeStep>
)
data class ScheduleEvent(
    val stepId: String, val startIso: String,
    val endIso: String, val type: String,
    val device: String, val instruction: String = ""
)
```

---

## 5. AI Entegrasyonu (Gemini 1.5 Flash)

### API Key Akışı
1. Setup ekranı tamamlandıktan sonra `ApiKeyOnboardingDialog` açılır (key boşsa).
2. Kullanıcı https://aistudio.google.com/apikey adresinden ücretsiz key alır.
3. Key, `SharedPreferences` içinde `"gemini_api_key"` anahtarıyla saklanır.
4. Key boşsa tüm AI fonksiyonları **mock/fallback** modda çalışır (uygulama crash etmez).

### AI Fonksiyonları (AppViewModel.kt)
| Fonksiyon | Prompt Formatı | Açıklama |
|-----------|---------------|----------|
| `startSession()` | `"Şu malzemelerle...3 farklı yemek öner. Format: 1\|En Kolay\|Ad\|Açıklama"` | Malzemelere göre 3 tarif seçeneği üretir |
| `refreshSession()` | `"Daha önce X önerdin. Bu kez onlardan FARKLI 3 yeni yemek üret."` | Mevcut malzemelerle yeni alternatifler üretir |
| `selectRecipeOption()` | `"Seçilen yemek: X. Donanım: Y. Askeri komutlarla tarif oluştur."` | Seçilen tarif için donanıma özel operasyon planı |
| `askIngredientAgent()` | `"Sen askeri şef asistanısın. Tarif: X. Kullanıcı sorusu: Y..."` | Malzeme değişikliği danışmanlığı |
| `scanIngredients()` | `"Resimdeki malzemeleri tespit et, virgülle listele"` | Bitmap → Gemini Vision → malzeme listesi |
| `checkVisionAgent()` | `"Askeri şef gibi fotoğrafı incele, ocağı kapat mı devam mı?"` | Pişirme sırasında görsel geri bildirim |

### getGeminiModel() Helper
```kotlin
private fun getGeminiModel(): GenerativeModel? {
    val key = _hw.value.geminiApiKey
    if (key.isBlank()) return null  // → fallback/mock moda düş
    return GenerativeModel("gemini-1.5-flash", key)
}
```

---

## 6. Loglama Sistemi (AppLogger.kt)

Merkezi singleton logger. Tüm AI istekleri, yanıtları, hatalar ve kullanıcı eylemleri loglanır.

### Kullanım
```kotlin
AppLogger.i("Component", "Bilgi mesajı")
AppLogger.w("Component", "Uyarı")
AppLogger.e("Component", "Hata", exception)
AppLogger.aiRequest("Feature", promptText)
AppLogger.aiResponse("Feature", responseText)
AppLogger.aiError("Feature", exception)
```

### Logları Çekme Komutları
```bash
# Logcat — gerçek zamanlı
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s AK

# Dosya olarak çekme
& $adb shell run-as com.agentickitchen.android cat files/agentic_log.txt
```

---

## 7. UI Akışı ve Navigasyon

```
App Başlatılır
  ↓
Setup tamamlandı mı? (SharedPreferences: "setup_done")
  ├─ HAYIR → SetupScreen (ekipman, porsiyon, saat, donanım)
  │           ↓ completeSetup()
  │           → API Key Dialog (key yoksa)
  │              ├─ Key girildi → saveHardwareSettings() → Ana Ekran
  │              └─ Geç → Mock mod → Ana Ekran
  └─ EVET → AppNavigation
               ├─ HomeScreen (BottomNav: Home)
               │   ├─ Malzeme girişi (TextField + Autocomplete + Kategori Grid)
               │   ├─ Kamera/Galeri tarama (CameraModal → scanIngredients)
               │   ├─ "Tarif Alternatifleri Üret" → startSession()
               │   ├─ 3 tarif kartı (OptionsReady) → kullanıcı seçer
               │   ├─ Hedef saat sorulur → selectRecipeOption()
               │   └─ RecipeActive: Adım listesi + Agent Chat + Vision Scan
               └─ SettingsScreen (BottomNav: Settings)
                   ├─ Pişirme Araçları (→ Setup'a geri)
                   ├─ Donanım Profili (HardwareDialog — ocak tipi, fırın, porsiyon, API key)
                   ├─ Diyet Tercihi (DietDialog)
                   ├─ Bildirimler (Switch)
                   ├─ Dil (ListDialog)
                   └─ Tema (ListDialog — green/blue/orange/dark)
```

---

## 8. Tema Sistemi (Theme.kt)

4 palette tanımlı: `PaletteGreen`, `PaletteBlue`, `PaletteOrange`, `PaletteDark`

```kotlin
data class AppColors(
    primary, primaryDark, primaryLight, accent,
    background, surface, onPrimary, onBackground,
    onSurface, onSurfaceSub, divider
)
// CompositionLocal ile erişim:
val colors = LocalAppColors.current
```

---

## 9. Geliştirme Tarihçesi ve Versiyonlar

### v1.0 - v1.5 (Prototip ve Temeller)
- Projenin iskeleti kuruldu, shared module ile KMP altyapısı hazırlandı.
- MVI mimarisi temelinde basit bir liste ve 3 adımlı (Yıka, Pişir, Bekle) sahte ajan planlayıcısı eklendi.
- Kategori ve Seçim UI'leri çok basitti, yatay scroll yorgunluğu yaratıyordu.

### v1.6 - v1.7 (Gelişmiş UX ve Temalandırma)
- **Dinamik Tema:** Sadece yeşil olan uygulama 4 farklı temaya ayrıldı (Mavi, Turuncu, Koyu Minimal).
- **Akıllı Otomatik Tamamlama:** TextField'a yazı yazılırken çalışan Autocomplete menüsü eklendi.
- **Diyet Profilleri:** Vegan, Vejetaryen, Keto ayarları Settings ekranına konuldu.

### v1.8 (Ergonomi ve Donanım Özelleştirmeleri)
- **Side-Tab Kategoriler:** Yatay kaydırma kaldırılarak sol tarafta sekmeler, sağ tarafta grid listesi olan Master-Detail (Side-Tab) tasarımına geçildi.
- **Kamera Simülasyonu:** Vision Agent için CameraModal prototipi eklendi.
- **Donanım Detayları:** Ocağın maksimum ısı seviyesi ve fırının özellikleri setup aşamasına eklendi.

### v1.9 (Askeri Netlikte AI ve Opsiyonlu Tarifler)
- **Çoklu Tarif Seçeneği:** AI artık tek tarif vermek yerine "En Kolay", "Comfort Food" ve "Sofistike" olmak üzere 3 seçenek sunuyor.
- **Hedef Zaman Sorma:** Kullanıcı tarife tıkladığında "Yemek ne zaman hazır olsun?" sorularak tersine zamanlama (Reverse-scheduling) hesaplanıyor.
- **Askeri Ama Anlaşılır Adımlar:** "serçe parmağının ucu büyüklüğünde", "ceviz büyüklüğünde tereyağı" gibi insani ama "Ocağı 8. seviyeye al" gibi askeri komutlar.
- **Gerçek Kamera & Galeri:** Kamera modalı Native Android "ActivityResultContracts" ile bağlandı.
- **Geri Dönüş (Navigasyon):** Aktif tarif içerisinden geriye basılarak diğer alternatiflere dönülebilme.
- **Ajan Chat + Vision Scan:** Tarif aktifken malzeme danışmanlığı ve görsel pişirme kontrolü.

### v1.9.3 (Gerçek AI Entegrasyonu)
- **Gemini 1.5 Flash SDK:** `com.google.ai.client.generativeai:generativeai:0.6.0` dependency eklendi.
- **Gerçek AI ile tarif üretimi:** `startSession()` ve `selectRecipeOption()` fonksiyonları Gemini API ile çalışıyor.
- **Gerçek Görsel İşleme:** Kamera/galeri bitmap'leri `Gemini Vision`'a gönderilip malzeme listesi parse ediliyor.
- **Aktif Tarif Gözlemcisi:** Pişirme sırasında foto çekildiğinde askeri şef personasıyla gerçek zamanlı tavsiye.
- Gemini 1.5 Flash SDK, AppLogger ve Merkezi API Key Yönetimi.

### v1.9.4 (ChefGPT & Hata Çözümleri)
- Uygulama ismi ChefGPT oldu, Model Fallback sistemi (2.5 -> 2.0) eklendi.

### v1.9.6 (Zen Precision UI Redesign & AI Fallback Fix)
- Zen Precision tasarım dili uygulandı ve UnknownException hata yönetimi güçlendirildi.

### v1.9.5 (Analog Heritage Redesign)
- Google Stitch tabanlı "Analog Heritage" tasarım sistemi (Krem kağıt, lacivert/bordo aksan).

### v1.10.0 (Three-Theme Intelligence Refresh & Operations Split)
- Theme metadata refactor, 4-tab navigation ve Pantry Intel agent sistemi eklendi.

### v1.11.0 (SQLDelight & Experience Polish)
- SQLDelight ile geçmiş kayıtları, Ktor tabanlı ağ istekleri ve Snackbar sistemi eklendi.

### v1.12.0 (AI Infrastructure Modernization & Multi-Provider)
- **Multi-Provider AI Architecture:** Gemini-only bağımlılığı kırılarak `LlmProvider` soyutlamasına geçildi.
- **New AI Providers:** Google Gemini, Hugging Face (Mistral), DuckDuckGo (GPT-4o-mini), Pollinations.ai.
- **Ktor Networking:** Uygulama katmanına Ktor Client ve Kotlinx Serialization entegre edildi.

### v1.12.1 (Real Free Vision & Stability)
- **Real Free Vision:** Gemini API anahtarı olmayan durumlar için `HuggingFaceVisionService` (Salesforce BLIP) üzerinden gerçek görsel analiz desteği eklendi.
- **Vision Orchestration:** Görselden metne (HF) -> Metinden malzemeye (DDG/Pollinations) şeklinde iki aşamalı "Keyless Vision" boru hattı kuruldu.
- **DuckDuckGo Fix:** SSE stream parsing mantığı Regex ile güçlendirilerek boş yanıt (0 char) hataları giderildi.
- **Graceful Fallback UI:** Görsel analiz sırasında hangi sağlayıcının kullanıldığına dair Snackbar bilgilendirmeleri eklendi.
- **API Key Provider Fix:** `ApiKeyOnboarding` artık seçili `aiProvider` temelinde doğru anahtarı kaydediyor; Gemini gerektirmeyen modlarda gereksiz key isteği tekrarlarını önlüyor.
- **Architecture Cleanup:** ViewModel içerisindeki mükerrer metodlar ve hatalı class kapanışları temizlendi.

### v1.12.2 (Free Provider Default) — AKTÜEL
- **API Key Gerekmez Artık:** Default AI provider `GEMINI` → `FREE` (Pollinations.ai / Mistral-7B) olarak değiştirildi.
- **Kullanıcı Dostu Başlangıç:** İlk açılışta API key dialog açılmaz; kullanıcı anında tarif üretebilir.
- **İsteğe Bağlı Premium:** Gelişmiş performans için Settings → Provider Seç → Google Gemini → key gir seçeneği açık.
- **Sağlayıcı Seçenekleri:** Ücretsiz: FREE (Mistral-7B), DUCKDUCKGO (GPT-4o-mini); Premium: GEMINI, HUGGINGFACE.
- **Vision Provider-Aware:** `scanIngredients()` ve `checkVisionAgent()` artık FREE/DUCKDUCKGO seçilmişse Gemini bypass edip doğru provider kullanıyor.

### v1.12.3 (Text-Based Vision Fallback) — AKTÜEL
- **HF Vision Hata Toleransı:** Hugging Face Vision servisi başarısız olduğunda (örnek: API limit, ağ), sistem artık metin tabanlı bir prompt ile malzeme tahmini yapıyor.
- **Akıllı Prompt Seçimi:** Eğer HF Vision başarıyla bir açıklama üretirse o açıklama üzerinden analiz yapılır; başarısız olursa "tipik yemek malzemeleri" genel bir liste üretilir.
- **Kullanıcı Bilgilendirmesi:** Görsel analizinin kaynağı (HF+Provider veya AI-metin) Snackbar ile bildirilir.
- **Güvenli Varsayılan:** Hiçbir servis çalışmasa bile kullanıcıya boş liste yerine anlamlı bir malzeme önerisi sunulur.

---

## 10. Bilinen Sorunlar ve Kısıtlamalar
- Tema değişikliğinde bazı ikon/yazı renkleri belirli paletlerde zor görülebilir (kontrast sorunu).
- DuckDuckGo sağlayıcısı SSE stream parsing kullandığı için çok uzun yanıtlarda nadiren kesinti yaşanabilir.
- FREE provider (Pollinations.ai) bazen yavaş yanıt verebilir (rate limiting); kritik uygulamalar için GEMINI + key önerilir.

---

## 11. Build ve Deploy Komutları

```powershell
# ── Test ──
.\gradlew.bat :shared:test

# ── Build APK ──
.\gradlew.bat :app-android:assembleDebug

# ── ADB ile Yükleme ──
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r "app-android\build\outputs\apk\debug\app-android-debug.apk"

# ── Uygulamayı Başlatma ──
& $adb shell am start -n com.agentickitchen.android/com.agentickitchen.android.MainActivity

# ── Logları Çekme ──
& $adb logcat -s AK                                                         # Gerçek zamanlı
& $adb shell run-as com.agentickitchen.android cat files/agentic_log.txt    # Dosya olarak
```

---

## 12. Gelecek Planları (Roadmap)
1. **SQLDelight Entegrasyonu:** [TAMAMLANDI]
2. **Kapsamlı Hata Yönetimi:** [TAMAMLANDI] Snackbar geri bildirimi eklendi.
3. **Multi-Provider AI:** [TAMAMLANDI] Hugging Face, DuckDuckGo, Pollinations eklendi.
4. **History UI:** [PLANLANIYOR] Mevcut `HistoryRepository` ve Compose listesiyle kolayca yapılabilir; düşük-orta seviye geliştirme çabası.
5. **Agent Sesli Geri Bildirim:** [PLANLANIYOR] Android TTS entegrasyonu gerekiyor; orta seviye iş yükü, adım kuyruğu ve ses durumu yönetimi gerekli.
6. **Offline Model (Local LLM):** [EXPERIMENTAL] Teknik olarak mümkün ama yüksek altyapı ve model indirme riski var; kısa vadede önceliği düşük.
