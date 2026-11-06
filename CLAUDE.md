# Tesbih Uygulaması – MVP Mimarisi


## Amaç ve kapsam

MVP, namaz sonrası tesbihatı ve serbest zikri dokunarak, titreşimle ve ses tuşlarıyla sayan, sayımı asla kaybetmeyen bir Android tesbih uygulamasıdır. Hedef, 8 Şubat 2027'de başlayan Ramazan'dan önce yayında olmaktır.

**MVP'de olanlar**

- Kayan boncuklu tesbih ekranı: dokunma, kaydırma, tık sesi, titreşim
- Tesbihat akışı: 33 Sübhanallah → 33 Elhamdülillah → 33 Allahu Ekber → yüze tamamlayan 1 tehlil, adımlar arası otomatik geçiş
- Hazır zikirler ve kullanıcının kendi zikri, hedef sayısıyla
- Ses tuşlarıyla sayma, ekranı açık tutma, karartma modu
- Günlük geçmiş (zikir başına toplam)
- Ayarlar: ses, titreşim, ses tuşları, ekranı açık tut, tema
- Türkçe ve İngilizce arayüz
- AdMob banner (yalnız Geçmiş ve Ayarlar ekranında) ve tek seferlik "Reklamları kaldır" satın alması

**Bilerek dışarıda bırakılanlar (sonraki sürümler)**

- Müezzin modu: müezzinin yaptıklarını adım adım izleten, müezzinlerin ve müezzinliği öğrenenlerin takip edeceği ayrı mod
- Wear OS saat uygulaması ve ana ekran widget'ı
- Ücretli tesbih modelleri (kehribar, oltu taşı vb.); boncuk görünümü yine de şimdiden veri olarak tanımlanır
- Hesap, bulut eşitleme, namaz vakitleri, hatırlatıcı bildirimler
- Ek diller (Arapça, Endonezce, Urduca)

Ekran tamamen kapalıyken sayım MVP'de yok: Android'de ses tuşları ekran kapalıyken uygulamaya güvenilir şekilde ulaşmaz. Onun yerine karartma modu var: ekran açık kalır ama siyah ve en düşük parlaklıktadır.

## Teknoloji seçimleri

Tek Activity, Jetpack Compose ve MVVM ile tek modüllü bir uygulama; kütüphane sayısı bilerek az tutulur.

| Konu | Seçim | Neden |
| --- | --- | --- |
| Dil | Kotlin | Mevcut deneyim |
| Arayüz | Jetpack Compose + Material 3, tek Activity | Canvas ile boncuk çizimi kolay |
| Mimari | MVVM + tek yönlü veri akışı (UiState + olaylar) | Test edilebilir, sade |
| Navigasyon | Navigation Compose | 4 ekran |
| Veritabanı | Room (KSP ile) | Zikirler ve günlük geçmiş |
| Ayarlar ve aktif oturum | DataStore (Preferences) | Küçük anahtar-değer verisi |
| Bağımlılık enjeksiyonu | Manuel `AppContainer` | MVP'de Hilt kurulumu gereksiz yük; büyüyünce Hilt'e geçilir |
| Asenkron | Coroutines + Flow | Standart |
| Ses | `SoundPool` | Düşük gecikmeli tık sesi |
| Titreşim | `VibrationEffect` | API 26+ |
| Reklam | Google Mobile Ads (AdMob) + UMP onay SDK'sı | Banner ve AB onayı |
| Satın alma | Google Play Billing | "Reklamları kaldır" |
| Test | JUnit + kotlinx-coroutines-test | Motor ve ViewModel testleri |
| minSdk | 26 (Android 8.0) | `VibrationEffect` ve `java.time` doğrudan kullanılır |
| compileSdk / targetSdk | Android Studio şablonunun verdiği | Play'in güncel hedef API şartı |

Sürümler `gradle/libs.versions.toml` içinde tutulur. Şablonun verdiği sürümler esas alınır; yeni kütüphane eklenirken sürüm uydurulmaz.

## Proje yapısı

Tek `app` modülü, özellik bazlı paketler; `core/domain` ve `core/content` Android'e bağımlı olmadığı için Wear OS geldiğinde ayrı modüle taşımak mekanik bir iş olur.

&#91;embedded content: bir dokunuşun akışı · 2 girdi, 4 çıktı\]

Dokunuş ekrana ve titreşime aynı karede yansır; diske yazma 300 ms gecikmeyle arka planda yapılır, bu yüzden hızlı sayımda takılma olmaz.

```
app/src/main/java/com/furkanbarissonmezisik/tesbihatpro/
├── TesbihApp.kt              // Application; AppContainer'ı kurar
├── MainActivity.kt           // tek Activity; ses tuşlarını yönlendirir
├── di/
│   └── AppContainer.kt       // tüm repository ve servislerin tek kaynağı
├── navigation/
│   └── AppNavHost.kt
├── core/
│   ├── domain/               // SAF KOTLIN: android.* / androidx.* import'u yasak
│   │   ├── model/            // Zikir, TesbihatStep, CounterState
│   │   └── engine/           // CounterEngine, CounterEvent, CounterEffect
│   ├── content/
│   │   └── PresetContent.kt  // hazır zikirler ve tesbihat; tüm dini metinler burada
│   ├── data/
│   │   ├── db/               // AppDatabase, entity'ler, DAO'lar
│   │   ├── prefs/            // SettingsStore, SessionStore (DataStore)
│   │   └── repository/       // ZikirRepository, HistoryRepository, SettingsRepository, SessionRepository
│   ├── feedback/             // HapticsController, SoundController
│   ├── input/                // VolumeKeyDispatcher
│   ├── ads/                  // AdsController (arayüz), AdMobAdsController, ConsentManager
│   ├── billing/              // PremiumRepository (arayüz), PlayBillingPremiumRepository, FakePremiumRepository
│   └── ui/                   // theme/, components/ (BannerAd vb.)
└── feature/
    ├── counter/              // CounterScreen, CounterViewModel, TesbihBeads (Canvas), DimOverlay
    ├── zikirs/               // ZikirListScreen, ZikirEditDialog, ZikirsViewModel
    ├── history/              // HistoryScreen, HistoryViewModel
    └── settings/             // SettingsScreen, SettingsViewModel
```

**Kurallar**

- `feature` paketleri birbirini import etmez; ortak her şey `core`'da durur.
- Her ekran üç parçadır: `XxxScreen` (durumsuz composable; UiState ve lambda alır), `XxxRoute` (ViewModel'i bağlar), `XxxViewModel` (`StateFlow<UiState>` yayınlar).
- ViewModel'ler bağımlılıklarını `AppContainer`'dan bir `ViewModelProvider.Factory` ile alır.

## Sayaç motoru

Sayma mantığının tamamı `CounterEngine` adlı saf Kotlin bir durum makinesindedir; ekran, veritabanı ve titreşim yalnızca onun çıktısını dinler.

```kotlin
data class Zikir(
    val id: Long,
    val arabic: String?,            // Arapça yazılış
    val transliterationTr: String,  // "Sübhânallah"
    val transliterationEn: String?,
    val meaningTr: String?,
    val meaningEn: String?,
    val defaultTarget: Int?,        // null = sınırsız sayım
    val isPreset: Boolean,
)

data class TesbihatStep(val zikirId: Long, val target: Int?)   // target null = sınırsız

data class CounterState(
    val steps: List<TesbihatStep>,  // tek zikir = tek adımlı liste
    val stepIndex: Int = 0,
    val count: Int = 0,
    val completed: Boolean = false,
)

sealed interface CounterEvent { data object Tap; data object Undo; data object ResetStep; data object ResetAll }
sealed interface CounterEffect { data object Tick; data class StepCompleted(val stepIndex: Int); data object SequenceCompleted; data object Milestone }

data class Transition(val state: CounterState, val effect: CounterEffect?)

object CounterEngine {
    fun reduce(state: CounterState, event: CounterEvent): Transition
}
```

**Davranış**

1. `Tap`: sayı 1 artar ve `Tick` döner. Sayı hedefe ulaşırsa `StepCompleted` döner; son adım değilse bir sonraki adıma geçilir ve sayı 0 olur. Son adımsa `completed = true` olur ve `SequenceCompleted` döner. `Transition` tek efekt taşır, en güçlüsü döner: `SequenceCompleted` > `StepCompleted` > `Milestone` > `Tick`.
2. `completed` iken `Tap` yok sayılır (efekt yok). Ekranda "Tamamlandı, yeniden başla" düğmesi `ResetAll` gönderir.
3. Sınırsız adımda (`target == null`) `Tap` hiç tamamlanmaz; her 33'te bir `Milestone` döner.
4. `Undo`: sayı > 0 ise 1 azalır. Sayı 0 ve önceki adım varsa o adıma `target - 1` ile dönülür. `completed` iken son adıma `target - 1` ile dönülür. Efekt yok.
5. `ResetStep`: sayı 0 ve `completed = false` olur. `ResetAll`: ilk adım, sayı 0, `completed = false`.

**Zorunlu birim testleri**: her olay için normal durum; adım geçişi; son adımda tamamlanma; tamamlanmışken Tap; adım sınırında Undo; tamamlanmışken Undo; sınırsız sayımda 33 ve 66'da Milestone; 1.000 ardışık Tap sonrası tutarlı durum.

**Hazır içerik** (`PresetContent.kt`, tek kaynak). Varsayılan tesbihat Diyanet fetvasına göredir: ilk üç satır 33'er, ardından yüze tamamlayan Tehlil 1 kez.

| Zikir | Arapça | Okunuş | Anlam | Varsayılan hedef |
| --- | --- | --- | --- | --- |
| Sübhanallah | سُبْحَانَ اللّٰهِ | Sübhânallah | Allah'ı her türlü eksiklikten tenzih ederim | 33 |
| Elhamdülillah | اَلْحَمْدُ لِلّٰهِ | Elhamdülillâh | Hamd Allah'a mahsustur | 33 |
| Allahu Ekber | اَللّٰهُ أَكْبَرُ | Allâhü ekber | Allah en büyüktür | 33 |
| Estağfirullah | أَسْتَغْفِرُ اللّٰهَ | Estağfirullâh | Allah'tan bağışlanma dilerim | 100 |
| Kelime-i tevhid | لَا إِلٰهَ إِلَّا اللّٰهُ | Lâ ilâhe illallâh | Allah'tan başka ilâh yoktur | 100 |
| Salavat | اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ | Allâhümme salli alâ seyyidinâ Muhammed | Allah'ım, efendimiz Muhammed'e salat eyle | 100 |
| Tehlil | لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ | Lâ ilâhe illallâh vahdehû lâ şerîke leh, lehü’l-mülkü ve lehü’l-hamdü ve hüve alâ külli şey’in kadîr | Allah’tan başka ilâh yoktur; yalnız Allah vardır. O tektir, ortağı yoktur. Mülk O’nundur, hamd O’na mahsustur. O’nun gücü her şeye yeter. | 1 |

Dini içerikte tek ölçü Diyanet'tir. Metinler yayından önce Diyanet kaynaklarından kontrol edilmeli. Claude Code kullanıcı istemedikçe bu tabloya metin eklemez, değiştirmez.

**Kaynaklar**

- Tesbihat sayıları, sırası, Tehlil'in Arapçası ve anlamı: [Din İşleri Yüksek Kurulu fetvası](https://kurul.diyanet.gov.tr/tr/fetva/namazlardan-sonra-tesbihat-nasil-yapilir/0193c42d-56d7-7df9-8636-ceba3a7927e6)
- Tehlil'in okunuşu: [Bayburt Müftülüğü](https://bayburt.diyanet.gov.tr/sayfalar/contentdetail.aspx?MenuCategory=Kurumsal&contentid=274)
- İngilizce: "Subhan Allah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah" yazımları ve "There is no god but Allah" Diyanet'in İngilizce hutbelerinden. Diğer İngilizce anlamlar Diyanet'in Türkçe anlamlarının çevirisidir. Estağfirullah, Salavat ve Tehlil'in İngilizce okunuşu için Diyanet'te İngilizce karşılık bulunamadı, Arapçadan yazıya aktarıldı.

## Veri katmanı

Zikirler ve günlük toplamlar Room'da, ayarlar ve aktif sayım DataStore'da durur; kural şu: uygulama kapansa, öldürülse, telefon yeniden başlasa da sayım kaybolmaz.

**Room tabloları**

| Tablo | Alanlar | Not |
| --- | --- | --- |
| `zikirs` | id, arabic, translit\_tr, translit\_en, meaning\_tr, meaning\_en, default\_target, is\_preset, sort\_order, created\_at | Hazırlar silinemez, düzenlenemez |
| `daily_counts` | date (yyyy-MM-dd, yerel saat), zikir\_id, count | Birincil anahtar (date, zikir\_id) |

Hazır zikirler ilk açılışta `PresetContent`'ten yüklenir. DataStore'daki `contentVersion` sayesinde sonraki sürümlerde yeni hazır içerik eklenebilir.

**DataStore**

- `SettingsStore`: soundEnabled, hapticsEnabled, volumeKeysEnabled, keepScreenOn, theme (sistem/açık/koyu), beadStyleId, isPremiumCache
- `SessionStore`: aktif adım listesi (zikir id + hedef), stepIndex, count, completed, updatedAt

**Sayımı kaybetmeme kuralı**

1. Sayım durumu bellekte, ViewModel'dedir; ekran anında güncellenir.
2. Her `Tap` / `Undo` sonrası `SessionStore` yazımı en geç 300 ms içinde yapılır. Gecikme her dokunuşta yeniden başlamaz; yeniden başlasaydı kesintisiz hızlı sayımda hiç yazım olmaz, çökmede tüm seri kaybolurdu.
3. Günlük toplam bellekte delta olarak birikir; aynı kuralla, en geç 300 ms içinde `count = count + delta` upsert edilir. `Undo` deltayı azaltır, toplam 0'ın altına inmez.
4. Uygulama arka plana geçince (`ProcessLifecycleOwner` ON\_STOP) bekleyen yazımlar anında yapılır.
5. Açılışta oturum `SessionStore`'dan geri yüklenir.
6. Tarih her yazımda `LocalDate.now()` ile hesaplanır; gece yarısını geçen oturum yeni güne yazılır.

Tüm yazımlar `Dispatchers.IO`'da yapılır; dokunuşun ekrana ve titreşime yansıması hiçbir zaman diske yazmayı beklemez.

## Ekranlar ve deneyim

Uygulama Tesbih ekranıyla açılır ve bu ekranda reklam yoktur; diğer üç ekrana üst bardaki ikonlarla gidilir (alt menü yok, sayım ekranı sade kalır).

| Ekran | İçerik | Reklam |
| --- | --- | --- |
| Tesbih (başlangıç) | Boncuklar, büyük sayı, adımın Arapçası ve okunuşu, adım göstergesi (1/3), geri al, sıfırla, karartma düğmesi | Yok |
| Zikirler | En üstte "Namaz sonrası tesbihat", altında hazır ve kendi zikirlerin; ekle, düzenle, sil (hazırlar hariç) | Yok |
| Geçmiş | Bugünün özeti, son 30 gün, gün başına zikir toplamları | Banner |
| Ayarlar | Ses, titreşim, ses tuşları, ekranı açık tut, tema, tesbih görünümü, reklamları kaldır, gizlilik seçenekleri, hakkında | Banner |

**Tesbih boncukları (`TesbihBeads`)**

- Compose `Canvas` ile çizilir: dikey, hafif kavisli bir ipte görünür yaklaşık 9 boncuk; ortadaki konum vurguludur.
- Ekranın herhangi bir yerine tek dokunuş = `Tap`. Dizi bir boncuk aralığı kadar aşağı kayar (spring, yaklaşık 150 ms).
- Hızlı dokunuşlarda animasyon kuyruğa girmez, doğrudan hedef konuma atlar; sayı hiçbir zaman animasyonu beklemez.
- Aşağı sürükleme bir boncuk aralığını geçince `Tap` sayılır.
- Adım hedefinde dizide daha büyük ve farklı biçimli imame boncuğu görünür.
- Görünüm veriyle tanımlanır: `BeadStyle(id, nameRes, baseColor, highlightColor, threadColor)`. MVP'de 2 ücretsiz stil (kehribar, koyu ahşap); gelecekteki ücretli modeller bu yapıya eklenir.

**Geri bildirim**

- `Tick`: hafif titreşim (API 29+ `EFFECT_TICK`, altında yaklaşık 10 ms) + `SoundPool` tık sesi
- `StepCompleted` ve `Milestone`: belirgin çift titreşim + farklı ses, ekranda bir sonraki zikrin adı
- `SequenceCompleted`: uzun titreşim deseni + "Tesbihat tamamlandı" mesajı ve yeniden başla düğmesi
- Ses ve titreşim ayarlardan ayrı ayrı kapatılabilir. Efektler olayla aynı karede, ana thread'de tetiklenir.

**Ses tuşları (`VolumeKeyDispatcher`)**

- Ayar açıkken ve Tesbih ekranı görünürken Ses Açma = `Tap`, Ses Kısma = `Undo`.
- `MainActivity.dispatchKeyEvent` olayı `VolumeKeyDispatcher`'a iletir. `CounterRoute` görünürken `DisposableEffect` ile kayıt olur, ekrandan çıkınca kaydı siler.
- Kayıtlı dinleyici yoksa olay sisteme bırakılır, normal ses ayarı çalışır.
- Basılı tutmada tekrarlanan olaylar (`repeatCount > 0`) sayılmaz.

**Ekranı açık tutma ve karartma**

- `keepScreenOn` açıkken yalnızca Tesbih ekranında `FLAG_KEEP_SCREEN_ON` uygulanır. Bayrak doğrudan pencereye yazılır; `View.keepScreenOn` durgun ekranda kalkmayabilir.
- Karartma modu: tam ekran siyah katman + pencere parlaklığı 0.01. Dokunma ve ses tuşları saymaya devam eder; çıkış 1 saniyelik uzun basmayla yapılır. Dokunuş burada parmak kalkınca sayılır, çıkış için yapılan uzun basış sayılmaz. `keepScreenOn` kapalı olsa da ekran açık kalır. Sistem çubukları gizlenmez: gizlemek Android'in "tam ekran" uyarısını açar ve ilk dokunuşu yutar.

**Dil ve erişilebilirlik**

- Varsayılan `values/` İngilizce, `values-tr/` Türkçe. Arapça metinler string kaynağı değil, `PresetContent` verisidir.
- TalkBack sayıyı ve adımı okur ("Sübhânallah, 12 / 33"); sistem yazı boyutuna uyum sağlanır.

## Reklam ve satın alma

Sayım ekranında hiçbir zaman reklam gösterilmez; gelir, Geçmiş ve Ayarlar ekranındaki banner ile tek seferlik "Reklamları kaldır" satın almasından gelir.

**İbadet kuralı**: İbadet sırasında açılabilecek ekranlarda (Tesbih, Zikirler, ileride müezzin modu) reklam olmaz. Bu ekranlarda reklam kodu hiç çalışmaz, reklam için ağ trafiği de oluşmaz; yeni ekran eklerken bu soru sorulur.

**Onay ve başlatma**

- `ConsentManager` (UMP) uygulama açılışında değil, ilk reklamlı ekrana (Geçmiş veya Ayarlar) girişte çalışır: onay bilgisini günceller, gerekiyorsa formu gösterir. Form açılışta çıksaydı ibadet sırasında Tesbih ekranını kapatırdı. `canRequestAds` true olmadan reklam isteği yapılmaz.
- `MobileAds` onaydan sonra arka planda başlatılır.
- Ayarlarda, UMP gerektiriyorsa "Gizlilik seçenekleri" girişi gösterilir.

**Banner**

- Uyarlanabilir (adaptive) banner (`AdSize.getLargeAnchoredAdaptiveBannerAdSize`), yalnızca Geçmiş ve Ayarlar ekranlarının altında.
- `isPremium` true ise hiç yüklenmez.
- Debug derlemede yalnızca Google'ın resmi test reklam birimi ID'leri kullanılır. Gerçek App ID ve birim ID'leri `local.properties`'ten (`admob.appId`, `admob.bannerUnitId`) okunup yalnız release derlemesine manifest placeholder / `BuildConfig` ile verilir; repoya girmez. Anahtarlar yoksa release de test kimlikleriyle derlenir, yayından önce mutlaka girilmeli.
- AB onay formunu denemek için `local.properties`'e `ump.testDeviceHash=<logcat'teki cihaz kimliği>` eklenir; yalnız debug'da etkilidir, denemeden sonra silinir.
- Kendi gerçek reklamına tıklamak AdMob hesabını riske atar; geliştirmede her zaman test ID.

**"Reklamları kaldır"**

- Play Console'da tek ürün: `remove_ads`, tek seferlik (tüketilmeyen) uygulama içi ürün.
- `PremiumRepository.isPremium: StateFlow<Boolean>`; açılışta ve `onResume`'da satın almalar sorgulanır.
- Satın alma mutlaka onaylanır (acknowledge); onaylanmayan satın almaları Google birkaç gün içinde iade eder.
- Sonuç `isPremiumCache`'e yazılır, çevrimdışı açılışta banner gizli kalır.

**Faz sırası için not**: Faz 1–7 boyunca `NoOpAdsController` ve `FakePremiumRepository` kullanılır. Böylece AdMob ve Billing kurulmadan uygulama eksiksiz çalışır.

**Kod dışında, AdMob konsolunda**: kumar, alkol ve flört gibi hassas kategorileri engelle. Uygulama "çocuklara yönelik değil" olarak işaretlenir.

## Uygulama fazları

Claude Code her seferinde tek faz uygular; her faz derlenen, çalışan bir uygulamayla biter ve kabul ölçütü sağlanmadan sonraki faza geçilmez.

1. **İskelet**: version catalog'a bağımlılıklar (Navigation Compose, Room + KSP, DataStore, lifecycle-viewmodel-compose, lifecycle-process), tema, `TesbihApp`, `AppContainer`, `AppNavHost` ve 4 boş ekran.
    - Kabul: uygulama açılır, dört ekran arasında gidip gelinir.
2. **Domain ve motor**: modeller, `CounterEngine`, `PresetContent`, birim testleri.
    - Kabul: "Sayaç motoru" bölümündeki tüm testler geçer.
3. **Sade sayaç ekranı**: büyük sayı, tam ekran dokunma, geri al, sıfırla, titreşim, ses, `SessionStore` kalıcılığı (henüz boncuk yok).
    - Kabul: sayarken uygulama görev yöneticisinden kapatılıp açılınca sayı ve adım aynıdır.
4. **Boncuk animasyonu**: `TesbihBeads`, sürükleme, imame, `BeadStyle`.
    - Kabul: saniyede 8 dokunuşta sayı ve boncuk konumu tutarlı, belirgin takılma yok.
5. **Zikirler**: Room, hazır içerik yükleme, liste, ekle/düzenle/sil, seçilen zikri veya tesbihatı sayaçta açma.
    - Kabul: tesbihat 33-33-33-1 adımlarıyla baştan sona çalışır; kendi zikrim kaydedilip sayılır.
6. **Geçmiş**: `daily_counts`, gecikmeli yazım, son 30 gün ekranı.
    - Kabul: sayılanlar Geçmiş'te doğru toplamla görünür; Undo toplamı düşürür.
7. **Ayarlar ve girdi**: `SettingsStore`, ses tuşları, ekranı açık tut, karartma modu, tema.
    - Kabul: her ayar uygulama yeniden açılınca korunur; ayar kapalıyken ses tuşları sesi değiştirir.
8. **Reklam**: UMP ve AdMob banner (test ID'leri), gerçek `AdMobAdsController`.
    - Kabul: banner yalnız Geçmiş ve Ayarlar'da görünür; Tesbih ve Zikirler ekranında hiç reklam isteği yapılmaz.
9. **Satın alma**: Play Billing, `remove_ads`, onaylama, banner'ın gizlenmesi.
    - Kabul: lisans test hesabıyla satın alınınca banner kaybolur ve yeniden açılışta gizli kalır.
10. **Yayın hazırlığı**: uygulama ikonu, release build type, R8 ve keep kuralları, sürüm kodu, gizlilik politikası metni taslağı. Şablondaki `packageScope` satırı release derlemeyi açılışta `IllegalAccessError` ile çökertiyor; satır kaldırılınca uygulama açılıyor, burada çözülür.
    - Kabul: `./gradlew bundleRelease` imzasız da olsa hatasız tamamlanır; release APK'da sayım ve reklam çalışır.

## Claude Code çalışma kuralları

Bu bölüm Claude Code'a doğrudan talimattır; dosya proje kökünde `CLAUDE.md` olarak durduğu için her oturumda okunur.

- Her seferinde yalnızca istenen fazı uygula. Faz sonunda `./gradlew assembleDebug` ve `./gradlew test` geçmeli; ardından kısa bir özet ver ve dur.
- İbadet sırasında açılabilecek ekranlara reklam veya reklamla ilgili kesinti koyma.
- Bu dokümanda olmayan bir kütüphane ekleme; gerekiyorsa önce sor.
- Bağımlılıkları yalnızca `gradle/libs.versions.toml` üzerinden ekle. Emin olmadığın bir sürümü uydurma, sor.
- `core/domain` ve `core/content` içinde `android.*` veya `androidx.*` import'u olmaz.
- Composable'larda iş mantığı yok: composable olay gönderir, ViewModel motor ve repository'leri çağırır.
- Kullanıcıya görünen her metin `strings.xml`'de (`values/` İngilizce, `values-tr/` Türkçe). Dini metinler yalnızca `PresetContent.kt`'de durur; yeni dini metin ekleme, mevcutları değiştirme.
- Dini konularda (metin, sayı, sıra, hüküm) tek ölçü Diyanet'tir: Din İşleri Yüksek Kurulu, Diyanet yayınları ve müftülükler. Başka kaynağa dayanma; Diyanet'te karşılığı yoksa sor.
- Gerçek AdMob ID'leri, keystore ve imzalama bilgileri koda veya repoya girmez.
- Hataları yutma; Room, DataStore, AdMob ve Billing hatalarını `Log` ile kaydet.
- Her faz ayrı commit: `faz-3: sade sayaç ekranı` biçiminde.
- **Commit'lere asla kendini ekleme.** Commit mesajlarına ve PR açıklamalarına `Co-Authored-By: Claude ...` satırı, "Generated with Claude Code" ibaresi ya da başka bir Claude/Anthropic atfı yazma. Commit'lerin tek yazarı git config'deki kullanıcıdır.
- Bu dokümanla çelişen ya da belirsiz bir durumda tahmin etme, sor.

**Commit atfını kapatma (Faz 1'de kurulur).** Yukarıdaki kural tek başına yetmeyebilir; Claude Code atfı varsayılan olarak ekler. Bu yüzden iki katman daha kurulur.

1. Proje kökünde `.claude/settings.json`:

```json
{
  "attribution": {
    "commit": "",
    "pr": ""
  }
}
```

2. Garanti için `.git/hooks/commit-msg` kancası; hangi yoldan gelirse gelsin atıf satırlarını commit mesajından siler:

```sh
#!/bin/sh
sed -i \
  -e '/^Co-[Aa]uthored-[Bb]y:.*[Cc]laude/d' \
  -e '/noreply@anthropic\.com/d' \
  -e '/[Gg]enerated with.*Claude Code/d' \
  "$1"
```

## Geleceğe hazırlık

Müezzin modu, saat uygulaması ve ücretli tesbih modelleri için MVP'de ek kod yazılmaz; yalnızca üç kural korunur: dini metinler tek yerde, sayma mantığı Android'den bağımsız, boncuk görünümü veri olarak.

| Gelecek özellik | Nasıl eklenir | MVP'de bunun için yapılan |
| --- | --- | --- |
| Müezzin modu | Yeni `feature/muezzin` paketi: müezzinin yaptıklarını sırasıyla izletir, müezzinler ve müezzinliği öğrenenler aktif takip eder. Kamet metni büyük puntoyla, satır satır, tam ekran. İçerik Diyanet'e göre hazırlanır. 100'lük tesbih sayımından ayrı akıştır, sayıma adım eklenmez. Ekranı açık tutma ve karartma altyapısını kullanır. | Dini metinler `core/content`'te; ekranı açık tutma mantığı `core`'da |
| Wear OS saat uygulaması | `core/domain` ve `core/content` ayrı bir `:core` modülüne taşınır; yeni `:wear` modülü (Compose for Wear OS) aynı `CounterEngine`'i kullanır. Telefonla eşitleme ayrı bir iş. | `core/domain`'de Android import'u yok |
| Ücretli tesbih modelleri | `BeadStyle`'a `productId` eklenir; `PremiumRepository` birden çok ürünü tanır. | Boncuk görünümü `BeadStyle` verisi |
| Hilt'e geçiş | Modül sayısı üçü geçince `AppContainer` Hilt modüllerine bölünür. | Tüm bağımlılıklar tek `AppContainer`'da |

## Yayın öncesi kontrol listesi

Bunlar kod dışı işlerdir ve senin yapman gerekir; hedef, Ramazan'dan önce sıralama toplamak için Jan 15, 2027 tarihinde yayında olmak.

- [ ] Paket adı kesinleşti (Play'de sonradan değiştirilemez)
- [ ] Hazır zikir metinleri Diyanet kaynaklarından kontrol edildi
- [ ] AdMob: gerçek ID'ler yalnız release derlemesinde; hassas kategoriler engellendi
- [ ] UMP onay formu AB bölgesi simülasyonuyla (debug geography) test edildi
- [ ] `remove_ads` ürünü Play Console'da oluşturuldu; lisans test hesabıyla satın alma denendi
- [ ] Gizlilik politikası sayfası yayında (reklam kimliği kullanımı belirtildi)
- [ ] Data safety formu reklam SDK'sının topladığı verilere göre dolduruldu
- [ ] Hedef kitle: çocuklara yönelik değil; içerik derecelendirme anketi tamamlandı
- [ ] Kapalı testte en az 1 hafta gerçek kullanım; çökme ve sayım kaybı kontrol edildi
- [ ] Mağaza sayfası Türkçe ve İngilizce; açıklamada "tesbih", "zikirmatik", "tasbih", "dhikr" geçiyor; ekran görüntüleri hazır
