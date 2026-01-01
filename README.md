# Kentin Sesi

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)

**Kentin Sesi**, vatandaşların yaşadıkları şehirdeki sorunları raporlayabildiği, takip edebildiği ve yerel yönetime dair katılımcı bir kültürün gelişmesine katkıda bulunduğu bir **civic tech** (sivil teknoloji) mobil platformudur. Bu proje **açık kaynak** ve **kâr amacı gütmeyen** bir sosyal girişimdir.

<div align="center">

Made with ❤️ by [zibidiSoft](https://github.com/zibidiSoft)

</div>

---

## 📋 İçindekiler

- [Civic Tech Nedir?](#-civic-tech-nedir)
- [Proje Hakkında](#-proje-hakkında)
- [Özellikler](#-özellikler)
- [Teknoloji Stack](#-teknoloji-stack)
- [Kurulum](#-kurulum)
- [Katkıda Bulunma](#-katkıda-bulunma)
- [Lisans](#-lisans)
- [zibidiSoft Hakkında](#-zibidisoft-hakkında)

---

## 🌍 Civic Tech Nedir?

**Civic Technology** (Sivil Teknoloji), vatandaşların kamusal konularda daha aktif rol almasını, demokratik süreçlere katılımını ve yerel/merkezi yönetimlerle etkileşimini güçlendirmek amacıyla teknolojinin kullanılması anlamına gelir.

Dünya genelinde hızla büyüyen civic tech hareketi, **şeffaflık**, **hesap verebilirlik** ve **katılımcılık** ilkeleriyle hareket eder. Bu alandaki uygulamalar:

- 🗳️ Vatandaşların sorunlarını görünür kılmalarını sağlar (örn: 311 sistemleri, FixMyStreet)
- 📊 Kamu verilerini açık ve anlaşılır hale getirir (örn: açık veri platformları)
- 🤝 Topluluk katılımını ve sivil girişimleri destekler
- 📢 Yerel ve merkezi yönetimlere dair bilgi akışını demokratikleştirir

**Kentin Sesi**, Türkiye'deki vatandaşların yerel sorunları dile getirmesi, takip etmesi ve çözüm süreçlerinde görünürlük sağlaması için geliştirilmiş bir civic tech örneğidir. Proje, global civic tech hareketinin **açık kaynak**, **şeffaf** ve **topluluk odaklı** değerlerini benimser.

---

## 🎯 Proje Hakkında

### Vizyon
Daha **şeffaf**, daha **hesap verebilir** ve daha **katılımcı** bir şehir yönetimi için teknolojiyi herkesin erişebileceği bir araca dönüştürmek.

### Misyon
- Vatandaşların **yerel sorunları** hızlıca raporlayabilmesini sağlamak
- Sorunların topluluk tarafından görünür kılınması ve desteklenmesiyle çözüm süreçlerine **kamusal denetim** ve **katılım** kazandırmak
- Açık kaynak yaklaşımıyla bu tür projelerin **incelenmesini**, **öğrenilmesini** ve **birlikte geliştirilmesini** teşvik etmek

### Hedef Kitle ve Coğrafi Kapsam

- **Hedef:** Türkiye (Pilot: Hatay)
- **Kullanıcı Profili:** Vatandaşlar, yerel yönetim görevlileri, uzmanlar
- **Global Adaptasyon:** Proje her ülkeye uyarlanabilir şekilde tasarlanmıştır

---

## ✨ Özellikler

### 🔐 Hesap Yönetimi
- E-posta/şifre ile kayıt ve giriş (Firebase Authentication)
- Çok aşamalı profil oluşturma (ad, kullanıcı adı, şehir, ilçe, unvan)
- DiceBear Avatar Sistemi: Her kullanıcı için otomatik oluşturulan benzersiz avatarlar
- Profil görüntüleme ve düzenleme ekranları

### 📝 Gönderi (Sorun) Paylaşımı
- **Kategori sistemi:** Sorunları organize etmek için önceden tanımlı kategoriler
- **Konum tabanlı:** Google Maps entegrasyonu ile konumlandırma
- **Durum takibi:** Yeni → İşlemde → Çözüldü

### 🔄 Durum Güncellemeleri (Status Updates)
- **Timeline görünümü:** Post'ların yaşam döngüsünü kronolojik takip
- **Manuel güncelleme:** Yetkili kullanıcılar durum değiştirebilir
- **Visual badges:** Durumlara göre renklendirilmiş etiketler

### 🏠 Akış ve Keşif
- **Ana akış:** Tüm gönderileri liste görünümünde görme
- **İnteraktif post kartları:** Upvote, yorum sayısı, durum göstergeleri
- **Gönderi detay:** Post içeriği, harita, yorumlar, durum güncellemeleri
- **Upvote (Destek) sistemi:** Gönderilere destek verme/geri alma

### 💬 Yorumlar ve Yanıtlar
- **Threaded comments:** Katmanlı yorum yapısı
- **Visual hiyerarşi:** Derinlik göstergeleri ve girintili görünüm
- **Yanıtlama sistemi:** Yorumlara doğrudan yanıt verme
- **Soft delete:** Yorumlar silinirken hiyerarşi korunur
- **Yazar bilgileri:** Ad, kullanıcı adı, şehir/ilçe, unvan

### 🗺️ Harita Deneyimi
- **Interactive map view:** Google Maps ile tüm gönderileri haritada görme
- **Cluster markers:** Yakın gönderileri gruplama
- **Location picker:** Post oluştururken konum seçme ekranı
- **Post detail map:** Detay ekranında embedded mini harita
- **Marker bilgileri:** Haritadaki işaretlere tıklayınca post detayına gitme

### 🔍 Filtreleme Sistemi
- **Çoklu filtreler:** İlçe, kategori, durum bazlı filtreleme
- **Dinamik UI:** Chip'ler ile aktif filtreleri gösterme
- **Filter presets:** Özel filtre kombinasyonlarını kaydetme
- **Preset yönetimi:** Kayıtlı filtreleri düzenleme ve silme
- **Real-time filtering:** Anında sonuç güncelleme

### 👤 Profil Ekranları
- **Kullanıcı profili:** Avatar, ad, kullanıcı adı, konum, unvan görüntüleme
- **İstatistikler:** Toplam paylaşım ve çözülen sorun sayıları
- **Tab sistemi:** "Paylaşımlarım" ve "Yorumlarım" görünümleri
- **Swipe refresh:** Profil verilerini yenileme
- **Dil ve tema ayarları:** Türkçe/İngilizce, Açık/Koyu/Sistem modu

### 🛠️ Admin Paneli
- **Rol bazlı erişim:** Admin ve moderatör rolleri
- **Karar yetkisi:** Post'ları onaylama, reddetme, silme
- **Durum yönetimi:** Post durumlarını değiştirme
- **Kullanıcı yönetimi:** (Geliştirilme aşamasında)
- **Dashboard:** Yönetim istatistikleri

### 🔔 Bildirimler
- **Geçici UI:** Mock bildirim ekranı (gerçek bildirim sistemi yakında)
- **Bildirim tipleri:** Yorum, yanıt, destek, durum güncellemesi, çözüldü
- **Visual indicators:** Tip bazlı iconlar ve renklendirme
- **Okunmamış göstergesi:** Yeni bildirimleri vurgulama

### 🎨 UI/UX Özellikleri
- **Material Design 3:** Modern ve temiz arayüz
- **Mint Green tema:** Özel renk paleti
- **Dark mode:** Tam karanlık mod desteği
- **Responsive layout:** Farklı ekran boyutlarına uyum
- **Smooth animations:** Geçişler ve etkileşimler
- **Türkçe/İngilizce:** Çoklu dil desteği

---

## 🛠️ Teknoloji Stack

### Platform ve Dil
- **Platform:** Android (minSdk 24, targetSdk 36)
- **Dil:** Kotlin 1.9+
- **Build System:** Gradle (Kotlin DSL)

### Mimari ve Pattern
- **Mimari:** MVVM (Model-View-ViewModel)
- **Repository Pattern:** Veri katmanı soyutlaması
- **Dependency Injection:** Hilt (Dagger 2 tabanlı)
- **Reactive Programming:** Kotlin Coroutines + Flow

### Firebase Services
- **Authentication:** E-posta/şifre ile kimlik doğrulama
- **Firestore:** NoSQL veritabanı (posts, users, comments, statusUpdates, filterPresets)
- **Storage:** Gönderi fotoğrafları için cloud depolama
- **Security Rules:** Rol bazlı erişim kontrolü

### UI ve Navigation
- **View Binding:** Type-safe view erişimi
- **AndroidX Navigation Component:** Fragment navigasyonu
- **Material Components:** Material Design 3 UI kütüphanesi
- **RecyclerView:** Liste görünümleri
- **Coil:** Görsel yükleme (SVG desteği ile)

### Harita ve Konum
- **Google Maps SDK:** Harita entegrasyonu
- **Google Places API:** Konum arama
- **Location Services:** GPS ve ağ tabanlı konum

### Veri Saklama
- **Room:** Yerel SQLite veritabanı
- **DataStore:** Key-value preferences
- **SharedPreferences:** Ayarlar ve tercihler

### Testing ve Quality
- **Firestore Rules Testing:** Güvenlik kuralı testleri
- **Linter:** Kod kalitesi kontrolü
- **Version Catalogs:** Dependency yönetimi

---

## 📦 Kurulum

### Gereksinimler

- **Android Studio:** Hedgehog (2023.1.1) veya daha yeni
- **JDK:** 17+
- **Android SDK:** 36
- **Firebase Projesi:** Auth, Firestore, Storage aktif
- **Google Cloud Project:** Maps SDK ve Places API aktif

### Adım Adım Kurulum

#### 1. Repoyu Klonlayın

```bash
git clone https://github.com/zibidiSoft/kentin-sesi.git
cd kentin-sesi
```

#### 2. Firebase Yapılandırması

**Kendi Firebase projenizle çalıştırmak için:**

1. [Firebase Console](https://console.firebase.google.com) üzerinden yeni proje oluşturun
2. Android uygulaması ekleyin (Package name: `io.github.thwisse.kentinsesi`)
3. `google-services.json` dosyasını indirin
4. Dosyayı `app/` dizinine kopyalayın
5. Firebase Console'da şu servisleri aktif edin:
   - **Authentication** → Email/Password
   - **Firestore Database**
   - **Cloud Storage**

#### 3. Firestore Security Rules

`firestore.rules` dosyasını Firebase'e deploy edin:

```bash
# Firebase CLI kurulumu (eğer yoksa)
npm install -g firebase-tools

# Firebase'e giriş
firebase login

# Projeyi bağlayın
firebase use --add

# Kuralları deploy edin
firebase deploy --only firestore:rules
```

#### 4. Google Maps API Key

1. [Google Cloud Console](https://console.cloud.google.com/) üzerinde Maps SDK ve Places API'yi aktif edin
2. API key oluşturun
3. Repo kökünde `local.properties` dosyası oluşturun (`.gitignore`'da zaten var):

```properties
MAPS_API_KEY=BURAYA_GOOGLE_MAPS_API_KEY_YAZIN
```

#### 5. Uygulamayı Çalıştırın

```bash
# Android Studio'da projeyi açın
# Build → Rebuild Project
# Run → Run 'app'
```

veya komut satırından:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## 🤝 Katkıda Bulunma

Kentin Sesi açık kaynak bir projedir ve katkıları memnuniyetle karşılar!

### Katkı Süreci

1. **Fork** yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'feat: Add amazing feature'`)
4. Branch'inizi push edin (`git push origin feature/amazing-feature`)
5. **Pull Request** açın

### Katkı Kuralları

- **Büyük değişiklikler için:** Önce bir Issue açıp tartışın
- **Kod stili:** Mevcut Kotlin kod stiline uyun
- **Commit mesajları:** [Conventional Commits](https://www.conventionalcommits.org/) kullanın
- **Testler:** Değişiklikleriniz için uygun testler ekleyin
- **Dokümantasyon:** README veya kod yorumlarını güncelleyin

### Önemli Notlar

- Projenin genel ürün yönü ve kararları **zibidiSoft** ekibi tarafından yönetilir
- Tüm katkılar GPLv3 lisansı ile lisanslanır
- Katkılarınız ile **civic tech** hareketine katkıda bulunuyorsunuz 🎉

---

## 📄 Lisans

Bu proje **GNU General Public License v3.0 (GPLv3)** ile lisanslanmıştır.

```
Kentin Sesi
Copyright (C) 2026 zibidiSoft

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

### Neden GPLv3?

Bu bir **sosyal girişim projesidir**. Kod tabanımızın:
- ✅ İncelenmesini
- ✅ Öğrenilmesini
- ✅ Geliştirilmesini

teşvik ediyoruz. **GPLv3** lisansını seçtik çünkü bu kodların alınıp, kapatılıp ticari bir ürüne dönüştürülmesine karşıyız. 

**Eğer bu kodu kullanacaksanız, sizin de açık kaynak kalmanız gerekir.**

Detaylı bilgi için: [LICENSE](LICENSE) dosyasına bakın.

---

## 🙏 Teşekkürler

Bu proje şu açık kaynak kütüphaneleri kullanmaktadır:

- [Firebase](https://firebase.google.com/)
- [Google Maps](https://developers.google.com/maps)
- [Hilt](https://dagger.dev/hilt/)
- [Coil](https://coil-kt.github.io/coil/)
- [DiceBear](https://www.dicebear.com/)
- [Material Components](https://material.io/)

---

<div align="center">

**Kentin Sesi**

Made with ❤️ by [zibidiSoft](https://github.com/zibidiSoft)

</div>
