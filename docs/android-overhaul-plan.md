# Android Uygulamasi Overhaul Plani

## 1. Mevcut Durum Analizi

### 1.1. Navigation (Mevcut)
- 5 tab'li bottom bar: Dashboard, Search, Notes, Tasks, People
- Her entity kendi list/detail/edit ekranina sahip
- Ayri ayri listeler halinde, entiteler arasi gecis baglamsiz
- Dashboard bir kontrol paneli gibi (istatistik kutulari, quick task, son notlar/son gorevler)

### 1.2. Veri Katmani
- Ktor ile backend'e baglaniyor
- Repository'ler `Result<T>` donuyor
- MVI mimarisi (StateFlow + sealed interface events)
- Tum entity'lerde `links` alani var ama UI'da kullanilmiyor

### 1.3. Tema
- Material You + material-kolor ile dinamik renk paleti
- Gradient background
- OLED modu, shading intensity gibi gelismis ozellikler var
- `ThemeState` ile runtime tema kontrolu

### 1.4. Tespit Edilen Hatalar / Eksikler

**Hata: Task ikonlari gosterilmiyor**
- `TaskListScreen.kt` satir ~100: `val displayIcon = if (task.icon.isNotEmpty()) task.icon else ...` - icon ADINI gosteriyor, icon'un kendisini degil
- `resolveIcon()` fonksiyonu var (`IconPicker.kt`) ama list/detail ekranlarinda cagrilmiyor
- Cozum: `resolveIcon(task.icon)` ile ImageVector'a cevirip `Icon()` ile gostermek

**Hata: Wikilink'ler islevsiz**
- `WikilinkText` sadece snackbar gosteriyor, navigasyon yapmiyor
- Backend `resolveWikilink` endpoint'i var ama Android tarafinda kullanilmiyor
- Kullanici `[[...]]` yazdiginda hicbir sey olmuyor

**Eksik: Entity baglanti UI'i**
- `links` field'i DTO'larda ve domain modellerinde var
- Create/Edit ekranlarinda "Baglanti Ekle" arayuzu tamamen eksik
- Kullanicilar not, gorev, kisi arasinda iliski kuramiyor

**Eksik: Rutin destegi**
- Sabah/Aksam rutini konsepti yok
- Zaman-farkindaligi yok (sabah sabah rutinini, aksam aksam rutinini gostermeli)
- Rutinler su an normal task olarak olusturulabiliyor ama ayri bir deneyim yok

**Eksik: Dashboard planner degil panel**
- Gunluk planlama, bugun yapilacaklar, zaman gosterimi yok
- Sadece genel istatistik ve son eklenenler var
- Rutin tamamlama akisi yok

## 2. Yeni Mimari - Genel Bakis

### 2.1. Navigation Yeniden Tasarimi

```
Bottom Bar:
[ Dashboard ]  [ Workspace ]  [ + (FAB) ]  [ Settings ]

- Dashboard: Gunluk planlayici, rutinler, bugunun gorevleri
- Workspace: Tum entity'lerin tam kontrol yonetimi (Notes, Tasks, People)
- FAB (ayri stil, ortadaki tab'a yapisik degil): Hizli olusturma menusu
- Settings: Tema, server, uygulama ayarlari
```

### 2.2. FAB Tasarimi

Bikram Agarwal'in Remember uygulamasindaki gibi:
- Bottom bar'dan bagimsiz, merkezde yukselen bir buton
- Uzerine basinca hizli olusturma secenekleri: Note, Task, Person
- Bottom bar'daki diger tab'lardan farkli bir renkte, belirgin

### 2.3. Ekran Haritasi

```
MainActivity
  └── AppNavigation
       ├── DashboardScreen (tab 1) - Gunluk Plan
       │    ├── QuickTaskRow
       │    ├── RoutineCard (sabah/aksam)
       │    ├── TodaysTasks
       │    └── QuickJournal
       │
       ├── WorkspaceScreen (tab 2) - Tum Yonetim
       │    ├── EntityTabs (Notes | Tasks | People)
       │    │    ├── ListScreen (filtered)
       │    │    ├── DetailScreen
       │    │    └── EditScreen (with LinkPicker)
       │    └── SearchBar (global, tum entity'ler)
       │
       ├── SettingsScreen (tab 3)
       │    ├── ThemeSection
       │    │    ├── Dark/Light/System
       │    │    ├── ColorSource (MaterialYou/Custom/Seed)
       │    │    ├── PaletteStyle
       │    │    ├── Gradient toggle
       │    │    └── OLED mode
       │    ├── ServerSection
       │    │    └── Base URL config
       │    └── AboutSection
       │
       ├── FABMenu (overlay)
       │    ├── New Note
       │    ├── New Task
       │    └── New Person
```

## 3. Implementasyon Siracesi (Phase'ler)

### PHASE 1: Kritik Hata DuZeltmeleri ve Infrastructure

#### 1.1 Task Ikon Gosterme Hatasi
**Dosyalar:** `TaskListScreen.kt`, `TaskDetailScreen.kt`, `DashboardScreen.kt`

- `resolveIcon()` fonksiyonunu task ikonu gosterilen her yerde kullan
- `TaskCard` component'ini duzelt: `Text(text = displayIcon)` yerine `Icon(imageVector = resolveIcon(task.icon), ...)`
- Fallback: icon yoksa veya bulunamazsa ilk harf goster

#### 1.2 Wikilink Navigasyonu
**Dosyalar:** `WikilinkText.kt`, `NoteDetailScreen.kt`, `TaskDetailScreen.kt`

- `onWikilinkClick` callback'ini tum entity tiplerini arastiran bir navigasyon mantigiyla degistir
- `resolveWikilink` API endpoint'ini kullan
- Backend'den gelen sonuca gore dogru ekrana navigate et

#### 1.3 Global Search'i Workspace'e Tasima
**Dosyalar:** Yeni `WorkspaceScreen.kt`, mevcut `SearchScreen.kt`

- Ayri bir Search tab'i kaldir
- Workspace'in ustunda global search bar yap
- Arama sonuclari kategorize ve entity tipine gore filtrele

### PHASE 2: Bottom Bar ve Navigasyon Yeniden Tasarimi

#### 2.1 Yeni Bottom Bar Bileseni
**Dosyalar:** Yeni `NavigationBar.kt`, `Screen.kt` (guncelle)

- 3 tab + 1 FAB: Dashboard, Workspace, Settings
- FAB bottom bar'in ortasinda ama yapisik degil, yuksekte
- Bikram Agarwal'in Remember uygulamasindaki gibi stil
- `Scaffold`'u guncelle

#### 2.2 Screen Rotasi Duzenlemesi
- Mevcut Screen sealed interface'i guncelle
- Workspace altinda entiteler arasi gecis icin alt navigasyon (tab row)
- Settings sayfasini ekle

#### 2.3 FAB Menu Bileseni
**Dosyalar:** Yeni `FabMenu.kt`

- 3 secenekli hizli olusturma: Note, Task, Person
- Animasyonlu acilma
- Her secenegin kendi ikonu ve rengi

### PHASE 3: Dashboard - Gunluk Planlayici

#### 3.1 Dashboard Yeniden Tasarimi
**Dosyalar:** `DashboardScreen.kt`, `DashboardViewModel.kt` (tamamen yeniden yaz)

Yeni layout:
```
+---------------------------+
| Selam, [isim]             |
| 14 Mayis 2025, Carsamba   |
+---------------------------+
| [Sabah Rutini] veya       |
| [Aksam Rutini] (zamana    |
|  gore otomatik)           |
|   [X] Meditasyon          |
|   [X] Kahvalti            |
|   [_] Gunluk yaz          |
|   [Tamamla]               |
+---------------------------+
| Bugunku Gorevler (3)      |
| [Task 1] [Status]         |
| [Task 2] [Status]         |
| [Hepsini Gor >]           |
+---------------------------+
| Hizli Not                 |
| [____________________] [+|
+---------------------------+
| Hizli Task                |
| [____________________] [+|
+---------------------------+
```

- StatsRow kaldir (gereksiz)
- Onun yerine bugune ait icerik

#### 3.2 Rutin Sistemi
**Dosyalar:** Yeni `RoutineRepository.kt`, `RoutineCard.kt`, domain model `Routine.kt`

- Rutin = sirali task'lerden olusan bir grup (template)
- Sabah/Aksam/WEEKDAY/WEEKEND ayrimi
- Backend'de tag-based veya ayri endpoint
- `GET /api/v1/routines?time=morning&day=monday` seklinde
- ViewModel'de zaman mantigi: su anki saat -> morning (06-12) / afternoon (12-17) / evening (17-22) / night (22-06)
- Her rutin adimi checkbox ile, tamamlanma yuzdesi goster

#### 3.3 Bugun Gorevleri
- `startDate <= today <= endDate` veya bugune ait task'leri goster
- Recurring task'leri dahil et
- Status: pending/in-progress/completed

### PHASE 4: Workspace - Tam Yonetim

#### 4.1 Workspace Ana Ekrani
**Dosyalar:** Yeni `WorkspaceScreen.kt`, `WorkspaceViewModel.kt`

- Ustte: Global search bar (mevcut SearchScreen'den)
- Ortada: TabRow ile Notes | Tasks | People gecisi
- Her tab icin ayri liste goruntusu
- Filtreleme ve siralama secenekleri

#### 4.2 Entity List'leri Iyilestirme
- Mevcut listeleri koru ama Workspace altina entegre et
- Liste elemanlarinda baglanti sayisini goster (ornegin "2 kisiver bagli")
- Swipe-to-delete opsiyonel

#### 4.3 Link Picker UI (En Onemli Ozelliklerden)
**Dosyalar:** Yeni `LinkPickerDialog.kt` veya `LinkPickerSheet.kt`

- Create/Edit ekranlarinda "Baglanti Ekle" butonu
- Tiklandiginda bir BottomSheet acilir
- Sheet icinde 3 sekme: Notes, Tasks, People
- Her sekmede arama yapilabilir
- Secilenler check isareti ile isaretlenir
- Secim tamamlaninca `links` alanina ID'ler eklenir
- Detail ekraninda baglantili varliklar gosterilir ve tiklanabilir

```
Ornek: Task Edit ekrani
+----------------------------+
| Task: Park Gezisi          |
| ...                         |
| [Baglanti Ekle]            |
|   Kisiler (2): [Ali][Ayse] |
|   Notlar (1): [Park Notu]  |
+----------------------------+
```

### PHASE 5: Settings ve Tema Iyilestirmeleri

#### 5.1 Settings Ekrani
**Dosyalar:** Yeni `SettingsScreen.kt`, `SettingsViewModel.kt`

- Server URL ayari (su an BuildConfig'te sabit)
- Tema secenekleri (mevcut ThemeState kullanilabilir hale getir)
- Dark mode: System/Light/Dark
- Color source: Material You / Custom Hex / Preset
- Gradient ac/kapa
- OLED modu
- Palette style secimi
- Shading intensity slider
- Uygulama hakkindda

#### 5.2 Material You Iyilestirme
- Mevcut tema sistemi zaten saglam, sadece Settings UI'i bagla
- Wallpaper'dan renk alimi duzgun calisiyor mu kontrol et
- `ThemeState`'i persistent hale getir (DataStore)

### PHASE 6: Baglanti Goruntuleme ve Wikilink Entegrasyonu

#### 6.1 Detail Ekranlarinda Baglantili Varliklar
- Note/Task/Person detail'de `links` alanini oku
- Backend'den bu ID'lerin baslik/info bilgilerini al
- Kart seklinde goster, tiklanabilir olsun
- Entity tipine gore renklendir

#### 6.2 Wikilink'ten Navigasyon
- `[[Park Gezisi]]` yazinca o isimde entity'yi bul
- Backend search veya ozel endpoint
- Birden fazla eslesme varsa secim dialog'u goster

### PHASE 7: CI/CD ve Build

#### 7.1 GitHub Actions Iyilestirme
- Mevcut `android-build.yml` calisiyor
- Release build ekle (signing gerekiyorsa)
- Lint kontrolu ekle (`./gradlew ktlintCheck`)
- APK artifact'ini koru

## 4. Degisiklik Listesi (Dosya Bazli)

### Silinecek Dosyalar
```
android/app/src/main/java/com/secondbrain/ui/search/SearchScreen.kt
android/app/src/main/java/com/secondbrain/ui/search/SearchViewModel.kt
```

### Yeni Dosyalar
```
android/app/src/main/java/com/secondbrain/ui/navigation/NavigationBar.kt    # Yeni bottom bar + FAB
android/app/src/main/java/com/secondbrain/ui/navigation/FabMenu.kt          # FAB menu overlay
android/app/src/main/java/com/secondbrain/ui/workspace/WorkspaceScreen.kt   # Workspace ana sayfa
android/app/src/main/java/com/secondbrain/ui/workspace/WorkspaceViewModel.kt
android/app/src/main/java/com/secondbrain/ui/settings/SettingsScreen.kt     # Ayarlar sayfasi
android/app/src/main/java/com/secondbrain/ui/settings/SettingsViewModel.kt
android/app/src/main/java/com/secondbrain/ui/dashboard/RoutineCard.kt       # Rutin karti
android/app/src/main/java/com/secondbrain/ui/dashboard/TodayTasksCard.kt    # Bugun gorevleri
android/app/src/main/java/com/secondbrain/ui/common/LinkPickerSheet.kt      # Baglanti secici
android/app/src/main/java/com/secondbrain/ui/common/LinkedEntitiesView.kt   # Baglantili varlik gostergesi
android/app/src/main/java/com/secondbrain/domain/model/Routine.kt           # Rutin domain modeli
android/app/src/main/java/com/secondbrain/data/api/RoutineApiService.kt     # Rutin API
android/app/src/main/java/com/secondbrain/data/repository/RoutineRepository.kt
```

### Degisecek Dosyalar
```
android/app/src/main/java/com/secondbrain/ui/navigation/Screen.kt           # Yeni route'lar
android/app/src/main/java/com/secondbrain/ui/navigation/AppNavigation.kt    # Yeni navigasyon yapisi
android/app/src/main/java/com/secondbrain/ui/dashboard/DashboardScreen.kt   # Tamamen yeniden
android/app/src/main/java/com/secondbrain/ui/dashboard/DashboardViewModel.kt # Tamamen yeniden
android/app/src/main/java/com/secondbrain/ui/tasks/TaskListScreen.kt        # Ikon hatasi duzeltme
android/app/src/main/java/com/secondbrain/ui/tasks/TaskDetailScreen.kt      # Ikon hatasi + link gosterme
android/app/src/main/java/com/secondbrain/ui/tasks/TaskEditScreen.kt        # LinkPicker entegrasyonu
android/app/src/main/java/com/secondbrain/ui/notes/NoteDetailScreen.kt      # Wikilink navigasyon + link gosterme
android/app/src/main/java/com/secondbrain/ui/notes/NoteEditScreen.kt        # LinkPicker entegrasyonu
android/app/src/main/java/com/secondbrain/ui/notes/NoteListScreen.kt        # Minimal degisiklik
android/app/src/main/java/com/secondbrain/ui/people/PersonListScreen.kt     # Minimal degisiklik
android/app/src/main/java/com/secondbrain/ui/people/PersonDetailScreen.kt   # Link gosterme
android/app/src/main/java/com/secondbrain/ui/people/PersonEditScreen.kt     # LinkPicker entegrasyonu
android/app/src/main/java/com/secondbrain/ui/util/IconPicker.kt             # Ek ikon ekleme (gerekirse)
android/app/src/main/java/com/secondbrain/ui/util/WikilinkText.kt           # Gercek navigasyon
```

## 5. Phase - Timeline

| Phase | Icerik | Tahmini Sure | Bagimlilik |
|-------|--------|-------------|------------|
| P1 | Kritik hata duzeltmeleri (ikon, wikilink) | 1-2 gun | Yok |
| P2 | Bottom bar + navigasyon yeniden tasarimi | 2-3 gun | P1 |
| P3 | Dashboard gunluk planlayici + rutinler | 3-4 gun | P2 |
| P4 | Workspace + LinkPicker | 4-5 gun | P2 |
| P5 | Settings + tema iyilestirme | 1-2 gun | P2 |
| P6 | Baglanti goruntuleme entegrasyonu | 2-3 gun | P4 |
| P7 | CI/CD, test, son kontroller | 1 gun | Hepsi |

**Toplam Tahmin: ~14-20 gun**

## 6. Teknik Detaylar

### 6.1 Bottom Bar + FAB Tasarimi (Bikram Agarwal Stili)

```kotlin
// Kavramsal yapi:
Scaffold(
    bottomBar = {
        Box {
            // Normal NavigationBar (Dashboard, Workspace, Settings)
            NavigationBar {
                NavigationBarItem(Dashboard)
                NavigationBarItem(Workspace)
                // Bosluk (FAB icin)
                Spacer(modifier = Modifier.width(48.dp))
                NavigationBarItem(Settings)
            }
            // FAB mutlak konumlandirma, bar'dan yuksekte
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-28).dp),
                onClick = { showFabMenu = true }
            )
        }
    }
)
```

### 6.2 LinkPicker Data Akisi

```
Kullanici "Baglanti Ekle" tiklar
  -> BottomSheet acilir (LinkPickerSheet)
     -> 3 tab: Notes, Tasks, People
     -> Her tab'da search + list
     -> Secilenler state'te tutulur
     -> "Kaydet" tiklaninca selectedIds doner
  -> EditViewModel.selectedLinks guncellenir
  -> Kaydedilirken `links` field'ina yazilir
```

### 6.3 Rutin Veri Modeli

Rutin'ler backend'de su an normal task olarak duruyor olabilir. Backend tarafinda da degisiklik gerekebilir. Ancak frontend'de gecici cozum:

- `recurrence` field'i olan + `routine` tag'li task'leri rutin olarak kabul et
- `tags` icinde "morning-routine" veya "evening-routine" ile ayirt et
- ViewModel'de saat dilimine gore hangi rutinleri gosterecegini belirle

Uzun vadede backende `GET /api/v1/routines` endpoint'i eklenmeli.

### 6.4 Ikon Hatasi Cozumu

```kotlin
// TaskListScreen.kt - Dogru kullanim:
val icon = resolveIcon(task.icon)
if (icon != null) {
    Icon(
        imageVector = icon,
        contentDescription = task.title,
        modifier = Modifier.size(24.dp),
        tint = onStatusColor
    )
} else {
    Text(
        text = task.title.take(1).uppercase(),
        color = onStatusColor
    )
}
```

Dashboard'daki `TaskCard` ve `TaskDetailScreen`'deki ikon gosterimi de ayni sekilde duzeltilecek.

### 6.5 Wikilink Navigasyon Akisi

```
Kullanici [[Metin]] tiklar
  -> WikilinkText.onClick fires
  -> viewModel.resolveWikilink("Metin") cagrilir
  -> API: GET /api/v1/search?q=Metin&exact=true
  -> Sonuc dondugunde:
     -> Tek sonuc -> direkt navigate
     -> Coklu sonuc -> secim dialog'u
     -> Hic sonuc yok -> snackbar "Bulunamadi"
```

## 7. Backend Gereksinimleri (Frontend Overhaul ile Birlikte)

1. `GET /api/v1/routines?time=morning&day=weekday` - Rutin endpoint'i
2. `GET /api/v1/entities/by-ids?ids=id1,id2,id3` - Toplu entity bilgisi (link gostermek icin)
3. `GET /api/v1/search/exact?q=Title&type=note` - Wikilink icin tam eslesme aramasi
4. `POST /api/v1/tasks/:id/complete` - Task tamamlama (recurring spawn icin)

## 8. GUI - Ekran Tasarim Referansi

Bottom bar ve genel estetik icin Bikram Agarwal'in Remember uygulamasi referans alinacak:

- **Bottom Bar**: 3 tab + merkezde yukselen FAB
- **Dashboard**: Minimal, beyaz alan bol, kartli yapi
- **Workspace**: Daha yogun, listeler, filtreler
- **Renkler**: Material You dinamik, gradient destegi

## 9. Riskler ve Notlar

1. **Rutin sistemi backend gerektiriyor** - Backend'de de degisiklik yapilmazsa, frontend gecici olarak tag-based cozum kullanabilir
2. **LinkPicker icin toplu entity API'i lazim** - Yoksa her link'li varlik icin ayri API cagrisi yapilir (performans sorunu)
3. **Mevcut dosyalari silmeden once branch al** - Her phase kendi branch'inde olmali
4. **Build'ler GitHub Actions uzerinden** - `push` ile otomatik APK olusuyor
5. **Emoji kullanma** - AGENTS.md geregi yasak
