# Kentin Sesi

Kentin Sesi, kâr amacı gütmeyen bir **sosyal girişim** ve **açık kaynak** mobil uygulama projesidir. Amaç; vatandaşların yaşadıkları şehirdeki sorunları görünür kılmasını, çözüm süreçlerini takip etmesini ve yerel yönetime dair katılımcı bir kültürün oluşmasını kolaylaştırmaktır.

---

## Proje Hakkında (Vizyon & Misyon)

### Vizyon
Daha **şeffaf**, daha **hesap verebilir** ve daha **katılımcı** bir şehir yönetimi için, teknolojiyi herkesin erişebileceği bir araca dönüştürmek.

### Misyon
- Vatandaşların **yerel sorunları** hızlıca raporlayabilmesini sağlamak.
- Sorunların topluluk tarafından görünür kılınması ve desteklenmesiyle, çözüm süreçlerine **kamusal denetim** ve **katılım** kazandırmak.
- Açık kaynak yaklaşımıyla, bu tür projelerin **incelenmesini**, **öğrenilmesini** ve **birlikte geliştirilmesini** teşvik etmek.

---

## Öne Çıkan Özellikler (Mevcut Durum)

Aşağıdaki özellikler halihazırda çalışır durumdadır:

- **Hesap Yönetimi**
  - E-posta/şifre ile kayıt ve giriş
  - Profil oluşturma ve güncelleme

- **Gönderi (Sorun) Paylaşımı**
  - Fotoğraf + başlık + açıklama + kategori ile sorun bildirimi
  - Konum bilgisiyle birlikte paylaşım
  - Gönderi durumları (ör. yeni / işlemde / çözüldü)
  - Gönderi silme

- **Akış (Feed)**
  - Gönderileri listeleme
  - Gönderi detay ekranı
  - Gönderilere “destek” verme (upvote)

- **Yorumlar ve Yanıtlar**
  - Gönderilere yorum ekleme
  - Yorumlara yanıt (threaded) ve hiyerarşik görüntüleme

- **Harita Deneyimi**
  - Gönderileri harita üzerinde görüntüleme
  - Konum seçici ekranı (Location Picker)

- **Filtreleme ve Kayıtlı Filtreler**
  - İlçe/kategori/durum bazlı filtreleme
  - Filtre preset’leri kaydetme/silme

- **Admin Paneli (Yetkili Kullanıcılar)**
  - Yönetim paneli ekranı ve rol bazlı aksiyon altyapısı

---

## Teknolojik Altyapı (Tech Stack)

Bu proje, modern Android geliştirme pratikleri üzerine kuruludur:

- **Dil:** Kotlin
- **Mimari:** MVVM + Repository Pattern
- **Dependency Injection:** Hilt
- **Firebase:**
  - Authentication
  - Firestore
  - Storage
- **Navigation:** AndroidX Navigation Component
- **Yerel Veri:** Room + DataStore
- **Harita:** Google Maps SDK

---

## Kurulum ve Katkı (Getting Started)

### Gereksinimler
- Android Studio (önerilen güncel sürüm)
- Android SDK (proje `compileSdk`/`targetSdk` 36)
- Bir Firebase projesi (Auth + Firestore + Storage)
- Google Maps API Key

### Projeyi Çalıştırma

1) Repoyu klonla:

```bash
git clone https://github.com/thwisse/kentin-sesi
```

2) Firebase yapılandırması:

- `app/google-services.json` dosyası proje içinde mevcut olmalı.
- Kendi Firebase projenle çalıştırmak istiyorsan Firebase Console’dan yeni bir `google-services.json` indirip `app/` altına koymalısın.

3) Google Maps API key:

Bu proje `MAPS_API_KEY` değerini `local.properties` içinden okur ve Manifest’e taşır.

Repo kökünde `local.properties` içine şunu ekle:

```properties
MAPS_API_KEY=BURAYA_GOOGLE_MAPS_API_KEY
```

4) Uygulamayı çalıştır:

- Android Studio -> Run

---

## Katkıda Bulunma (Contributing)

Bu proje açık kaynaktır ve katkıları memnuniyetle karşılar.

- Pull Request (PR) açabilirsin.
- Büyük değişiklikler için önce bir Issue açıp tartışmak önerilir.
- Projenin genel ürün yönü ve kararları **ana ekip** tarafından yönetilir.

---

## Lisans ve Felsefe

Bu proje **GNU General Public License v3.0 (GPLv3)** ile lisanslanmıştır.

**Neden GPLv3?**

Bu bir halk projesidir. Kodlarımızın incelenmesini, öğrenilmesini ve geliştirilmesini teşvik ediyoruz. **GPLv3** lisansını seçtik çünkü bu kodların alınıp, kapatılıp ticari bir ürüne dönüştürülmesine karşıyız. Eğer bu kodu kullanacaksanız, sizin de **açık kaynak** kalmanız gerekir.

Detay için: `LICENSE`
