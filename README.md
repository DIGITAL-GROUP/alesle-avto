# Alesle Avto

**Alesle Avto** — telefoningizni avtomobil ekraniga (head unit) Android Auto kabi ulaydigan dastur.
Xarita, musiqa, qo‘ng‘iroqlar va xabarlar avtomobil ekranida ko‘rinadi. Yandex Xaritalar, Yandex
Musiqa, YouTube kabi telefon ilovalarini ham avtomobil ekranida ochish mumkin.

- Google xizmatlari (Google Play Services) **kerak emas**: Xitoy proshivkali telefonlarda ham ishlaydi.
- Kabel (USB) orqali yoki **simsiz** (Bluetooth + Wi-Fi) ulanadi.
- Ekrandagi yozuvlar o‘zbek va ingliz tillarida.

> Alesle Avto is based on AARE by prototux (github.com/prototux/AARE), GPL-3.0-or-later.
> Alesle Avto prototux’ning AARE loyihasi asosida yaratilgan. Google bilan bog‘liq emas.
> Android Auto — Google LLC’ning savdo belgisi.

> [!WARNING]
> Dastur hali yangi. Avval mashina to‘xtab turganda sinab ko‘ring. Haydash paytida ekran bilan
> ovora bo‘lmang.

---

## Mundarija

1. [Nimalar kerak](#1-nimalar-kerak)
2. [O‘rnatish](#2-ornatish)
3. [Birinchi sozlash](#3-birinchi-sozlash)
4. [Avtomobilga ulash](#4-avtomobilga-ulash)
   - [A. Kabel (USB) orqali](#a-kabel-usb-orqali)
   - [B. Simsiz (wireless)](#b-simsiz-wireless)
   - [C. Zaxira yo‘l: telefon hotspot’i orqali](#c-zaxira-yol-telefon-hotspoti-orqali)
5. [Har safar mashinaga o‘tirganda](#5-har-safar-mashinaga-otirganda)
6. [Avtomobil ekranidan foydalanish](#6-avtomobil-ekranidan-foydalanish)
7. [Telefon ilovalari avtomobil ekranida (Yandex, YouTube)](#7-telefon-ilovalari-avtomobil-ekranida)
8. [Ovozni avtomobilga chiqarish](#8-ovozni-avtomobilga-chiqarish)
9. [Telefondagi sozlamalar](#9-telefondagi-sozlamalar)
10. [Muammo bo‘lsa](#10-muammo-bolsa)
11. [Texnik ma’lumot va litsenziya](#11-texnik-malumot-va-litsenziya)

---

## 1. Nimalar kerak

| Nima | Izoh |
|---|---|
| **Android telefon** | Android 8 yoki yangisi. Telefon ilovalarini avtomobil ekranida ochish uchun Android 12+. |
| **Avtomobil ekrani (head unit)** | Android Auto’ni qo‘llaydigan ekran, yoki **Open Headunit** dasturi o‘rnatilgan Android magnitola. |
| **Shizuku** (ixtiyoriy) | Telefon ilovalari (Yandex Xaritalar va boshqalar) avtomobil ekranida ishlashi va ovoz uchun. [shizuku.rikka.app](https://shizuku.rikka.app/) |

---

## 2. O‘rnatish

Hozircha tayyor APK fayl e’lon qilinmagan. Ikki yo‘l bor:

**Yo‘l 1 — tayyor APK fayl bilan.** Kimdir sizga `app-debug.apk` faylini bergan bo‘lsa:

1. Faylni telefonga ko‘chiring va ustiga bosing.
2. Telefon "noma’lum manbadan o‘rnatish"ga ruxsat so‘rasa, ruxsat bering.
3. **O‘rnatish**ni bosing.

**Yo‘l 2 — kompyuterda o‘zingiz yig‘ish.** Kompyuterda Android SDK va JDK bo‘lishi kerak:

```sh
git clone git@github.com:DIGITAL-GROUP/alesle-avto.git
cd alesle-avto
./build.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> **Muhim:** yangilashda doim `adb install -r` ishlating. Dasturni **o‘chirib qayta o‘rnatmang**:
> ishonchli head unit’lar, ruxsatlar va Shizuku ruxsati o‘chib ketadi.

Dastur identifikatori `uz.alesle.avto`. U eski AARE (`net.prototux.aare`) bilan birga tura oladi.
Lekin ikkalasi bir vaqtda o‘rnatilgan bo‘lsa, ba’zi avtomobil ilovalari Alesle Avto’ni tanimaydi.
Shuning uchun AARE’ni o‘chirib qo‘ygan ma’qul. O‘chirgandan keyin telefonni bir marta qayta yoqing.

---

## 3. Birinchi sozlash

Alesle Avto’ni oching. Bosh ekranda **Finish setting up** (sozlashni tugatish) ro‘yxati turadi.
Har bir qatorni bosib, so‘ralgan ruxsatni bering:

1. **Permissions** — mikrofon, joylashuv, Bluetooth, yaqin atrofdagi qurilmalar (nearby devices),
   bildirishnomalar, qo‘ng‘iroqlar, kontaktlar, kalendar. Hammasiga **Ruxsat berish**ni bosing.
2. **Notification access** — xabarlar va "hozir nima ijro etilyapti" avtomobilda ko‘rinishi uchun.
   Ochilgan ro‘yxatda **Alesle Avto**ni yoqing.
3. **Car audio** — telefon ovozi avtomobil karnaylaridan chiqishi uchun
   (batafsil: [8-bo‘lim](#8-ovozni-avtomobilga-chiqarish)).

**Xiaomi / HyperOS / MIUI telefonlarda qo‘shimcha** (aks holda telefon dasturni orqa fonda
"uxlatib" qo‘yadi va simsiz ulanish o‘z-o‘zidan boshlanmaydi):

- *Sozlamalar → Ilovalar → Alesle Avto → Avtomatik ishga tushirish* (Autostart) — **yoqing**.
- *Sozlamalar → Ilovalar → Alesle Avto → Batareya* — **Cheklovsiz** (No restrictions) ni tanlang.
- Oxirgi ilovalar ro‘yxatida Alesle Avto’ni **yuqoriga surib yopmang**: HyperOS bunda dasturni
  butunlay to‘xtatadi.

---

## 4. Avtomobilga ulash

### A. Kabel (USB) orqali

Eng oddiy va eng ishonchli yo‘l.

1. Telefonni avtomobilning Android Auto USB portiga (yoki Open Headunit o‘rnatilgan magnitolaga)
   kabel bilan ulang.
2. Telefonda "Alesle Avto’ni ochish?" degan savol chiqsa, **OK** ni bosing.
3. **Birinchi marta:** telefonda **"New head unit"** (yangi head unit) bildirishnomasi chiqadi.
   **Trust** (ishonish) tugmasini bosing, so‘ng kabelni chiqarib qayta ulang.
4. Avtomobil ekranida Alesle Avto paydo bo‘ladi.

> Faqat hozir o‘zingiz ulagan head unit uchun **Trust**ni bosing.

### B. Simsiz (wireless)

Simsiz ulanish uch qadamda ishlaydi: telefon avtomobil bilan **Bluetooth** orqali gaplashadi,
avtomobil o‘z **Wi-Fi** tarmog‘i nomi va parolini beradi, telefon o‘sha tarmoqqa ulanadi.

**Bir martalik tayyorgarlik:**

1. Telefonda **Bluetooth** va **Wi-Fi** yoqilgan bo‘lsin. Wi-Fi biror tarmoqqa ulanmagan
   bo‘lsa ham, **yoqilgan** bo‘lishi shart.
2. Telefonni avtomobil bilan Bluetooth orqali **bog‘lang** (pair): telefonning Bluetooth
   sozlamalarida avtomobil nomini (masalan, *Bakirov's*) tanlang.
3. Alesle Avto → **Connection** → **Wireless** yoqilgan bo‘lsin (odatda yoqilgan).
4. Head unit’da simsiz Android Auto yoqilgan bo‘lsin. **Open Headunit**da: Android Auto
   rejimini **WiFi** qilib qo‘ying va ekranda *"Waiting for your phone"* chiqsin.

**Ulanish:**

1. Mashinani yoqing. Telefon Bluetooth orqali avtomobilga o‘zi ulanadi.
2. Alesle Avto avtomobilni sezib, ulanishni o‘zi boshlaydi. Bildirishnomada
   *"Connecting wirelessly…"* yoki *"Waiting for … to offer wireless Android Auto…"* chiqadi.
   Head unit simsiz rejimni keyinroq yoqsa ham, dastur bir daqiqagacha kutadi.
3. Telefonda **"Searching for device…"** oynasi chiqadi. Ro‘yxatda avtomobil tarmog‘i (masalan,
   `DIRECT-QA-Android_45e4`) paydo bo‘lishini kuting, **ustiga bosing**, keyin **Connect**ni bosing.
   Head unit har safar bir xil tarmoqni ishlatsa, bu oyna faqat **birinchi marta** chiqadi.
   Open Headunit’da *Keep the same WiFi Direct network* yoqilgan bo‘lsin.
4. Bir necha soniyada avtomobil ekranida Alesle Avto paydo bo‘ladi.

**O‘zi ulanmasa — qo‘lda ulash:**

Alesle Avto → **Connection** → pastda **Your cars** → **Paired over Bluetooth**. Bu yerda
bog‘langan avtomobillar ko‘rinadi:

| Yozuv | Ma’nosi |
|---|---|
| **Bluetooth: connected / not connected** | Telefon hozir avtomobilga Bluetooth orqali ulanganmi |
| **Android Auto wireless: yes** | Avtomobil simsiz Android Auto’ni taklif qilyapti — ulanish mumkin |
| **Android Auto wireless: no** | Avtomobil bu xizmatni hozir taklif qilmayapti (head unit’da simsiz rejim o‘chiq) |

- **Refresh (SDP)** — avtomobildan xizmatlarini qaytadan so‘raydi. Head unit’da simsiz rejimni
  yoqqandan keyin bosing: yozuv **yes**ga o‘zgarishi kerak.
- **Connect wirelessly** — ulanishni hozir, qo‘lda boshlaydi.

> **Maslahat:** Wi-Fi signali zaif bo‘lsa (telefon head unit’dan uzoqda), ulanish sekin bo‘ladi
> yoki tasvir qotadi. Telefonni head unit’ga yaqin qo‘ying.

### C. Zaxira yo‘l: telefon hotspot’i orqali

Head unit simsiz ulanishni umuman qo‘llamasa (yoki telefon sertifikatini rad etsa), lekin unda
**Open Headunit** o‘rnatilgan bo‘lsa, bu yo‘l doim ishlaydi:

1. Telefonda **Hotspot** (modem rejimi)ni yoqing.
2. Head unit’ni telefon hotspot’iga Wi-Fi orqali ulang.
3. Alesle Avto → **Advanced** → **Head unit server**ni yoqing. Shu yerda:
   - **Accept from the network** — **yoqing**;
   - **Turn off network mode after a session** — **o‘chiring** (aks holda har safar qayta
     yoqishingiz kerak bo‘ladi).
4. Open Headunit’da **Headunit Server** rejimini tanlang. Telefon manzili sifatida hotspot
   manzilini kiriting: head unit’ning Wi-Fi sozlamalarida "gateway / router" deb ko‘rsatiladi.
   Port: **5277**.
5. Open Headunit’da ulanishni boshlang.

> Ogohlantirish: **Accept from the network** yoqilganda, telefoningiz bilan bir Wi-Fi’dagi
> istalgan qurilma parolsiz ulanishi mumkin. Buni faqat o‘z hotspot’ingizda ishlating.

---

## 5. Har safar mashinaga o‘tirganda

**Kabel bilan:** telefonni USB’ga ulang. Tamom.

**Simsiz:**

1. Telefonda **Bluetooth** va **Wi-Fi** yoqilgan bo‘lsin.
2. Mashinani yoqing. Head unit’da Open Headunit **WiFi** rejimida bo‘lsin.
3. 10–60 soniya kuting: Alesle Avto o‘zi ulanadi.
4. Ulanmasa: Alesle Avto → **Connection** → **Your cars** → **Connect wirelessly**.

---

## 6. Avtomobil ekranidan foydalanish

**Bosh ekran (Dashboard):** xarita yoki yo‘nalish, hozir ijro etilayotgan musiqa, keyingi
kalendar tadbiri. Orqa fonga rasm (wallpaper) qo‘yish mumkin.

**Panel (chetdagi yoki pastdagi qator):**

- **Orqaga** (←) — oldingi ekranga qaytadi. Telefon ilovasida "orqaga" bosadi; ilovada boshqa
  orqaga qaytadigan joy qolmasa, ilovalar menyusiga qaytadi.
- **Menyu / Bosh sahifa** — ilovalar ro‘yxati yoki bosh ekran.
- **Ovoz tugmasi** — ovozli yordamchi.
- O‘rtada: navigatsiya, musiqa, oxirgi ishlatilgan ilovalar, **Telefon** va **Sozlamalar**.
- Chetda: soat, ob-havo, telefon signali, batareya, bildirishnomalar.

Avtomobilning o‘z "orqaga" tugmasi ham xuddi shunday ishlaydi.

**Ilovalar menyusida ikki bo‘lim:**

- **Avto ilovalar** — avtomobil uchun maxsus ilovalar (CoMaps, Organic Maps, musiqa ilovalari va h.k.).
- **Telefon ilovalari** — telefondagi istalgan ilova (Yandex Xaritalar, Yandex Musiqa, YouTube…).
  Yuqoridagi qidiruv maydoniga ilova nomini yozing: natijalar klaviatura ustida chiqadi.

**Ichki ilovalar:**

- **Telefon** — oxirgi qo‘ng‘iroqlar, sevimli kontaktlar, raqam terish (to‘xtab turganda).
  Kiruvchi qo‘ng‘iroqda **Answer** (javob) va **Decline** (rad etish) chiqadi.
- **Bildirishnomalar** — oxirgi xabarlar, ovoz chiqarib o‘qish.
- **Sozlamalar** — ranglar, panel joylashuvi, uslub.

**Haydash paytida** xavfsizlik uchun ba’zi narsalar (yozish, uzun ro‘yxatlar, telefon
ilovalari) cheklanadi: faqat navigatsiya va musiqa ilovalari qoladi. Yo‘lovchi ishlatayotgan
bo‘lsa: telefonda *Car screen → Driving → Passenger mode*. Haydovchi esa haydash paytida
ekranga tegmasligi kerak.

---

## 7. Telefon ilovalari avtomobil ekranida

Yandex Xaritalar, Yandex Navigator, Yandex Musiqa, YouTube kabi ilovalar avtomobil ekranida
**to‘liq ishlaydi**: barmoq bilan bosish, surish, ikki barmoq bilan kattalashtirish. Ular
telefonning o‘z ekranini band qilmaydi: telefon qulflangan bo‘lsa ham ishlaydi.

Buning uchun **Shizuku** kerak (Android 12+):

1. Telefonga **Shizuku** ilovasini o‘rnating.
2. Shizuku’ni ishga tushiring (*Wireless debugging* orqali). Bu telefon har qayta yoqilganda
   **bir marta** qilinadi.
3. Avtomobil ekranida **Telefon ilovalari** bo‘limidan ilovani birinchi marta ochganda
   **Ruxsat berish**ni bosing va telefonda Shizuku so‘rovini tasdiqlang.

Ilova avtomobil ekranida doim tik turadi va aylanmaydi. Ekranning bo‘sh qolgan chetlari to‘q
rangda bo‘ladi.

---

## 8. Ovozni avtomobilga chiqarish

Alesle Avto → **Car audio**. Telefoningiz qo‘llaydigan eng yaxshi usulni tanlang:

| Usul | Nima kerak | Navigatsiya ovozi |
|---|---|---|
| **Shizuku** | Shizuku ishlab turgan bo‘lishi, Android 12+ | ha, musiqa bilan birga |
| **Ekranni yozib olish ruxsati** | bir marta ruxsat (*Butun ekran*ni tanlang) | yo‘q, telefonda qoladi |

Ba’zi avtomobillar telefon ovozini Bluetooth orqali ijro etaveradi: unda bu sozlamani bo‘sh
qoldiring.

---

## 9. Telefondagi sozlamalar

Alesle Avto bosh ekranidagi bo‘limlar:

| Bo‘lim | Nima uchun |
|---|---|
| **Car screen** | Ranglar, panel qayerda turishi, orqa fon rasmi, ob-havo, yo‘lovchi rejimi |
| **Apps** | Avtomobilda qaysi ilovalar va qaysi tartibda ko‘rinishi |
| **Voice assistant** | Ovoz tugmasi qaysi yordamchini ochishi |
| **Messages** | Xabarlar bildirishnomasi, ovoz chiqarib o‘qish, haydash paytida "Bezovta qilmang" |
| **Car audio** | Telefon ovozi avtomobilga qanday yetib borishi |
| **Connection** | Simsiz ulanish, video sifati, avtomobillaringiz, qo‘lda simsiz ulanish |
| **Advanced** | Head unit server, sertifikatlar, ulanish jurnali (log) |

**Preview car screen** tugmasi avtomobil ekranini telefonning o‘zida ko‘rsatadi: mashinasiz
sinab ko‘rish uchun.

---

## 10. Muammo bo‘lsa

Bosh ekrandagi qizil **Last error** (oxirgi xato) yozuvi nima bo‘lganini aytadi.

| Xato yoki holat | Sababi | Nima qilish kerak |
|---|---|---|
| *"… doesn't offer wireless Android Auto over Bluetooth"* | Head unit’da simsiz rejim yoqilmagan | Open Headunit’ni **WiFi** rejimiga qo‘ying, keyin *Your cars → Refresh (SDP)* va *Connect wirelessly* |
| *"Wi-Fi is off on the phone"* | Telefonda Wi-Fi o‘chiq | Wi-Fi’ni yoqing va qayta ulaning |
| *"Couldn't join the car's Wi-Fi …"* | Tarmoq topilmadi yoki **Connect** bosilmadi | Telefonni head unit’ga yaqinlashtiring. *"Searching for device"* oynasida tarmoqni tanlab **Connect**ni bosing |
| *"The head unit refused this phone's certificate"* | Head unit faqat Google imzolagan telefonlarni qabul qiladi | Qayta ulaning (dastur boshqa sertifikatni taklif qiladi). Bo‘lmasa: [4-C zaxira yo‘l](#c-zaxira-yol-telefon-hotspoti-orqali) yoki [sertifikatlar haqida](docs/certificates.md) |
| *"head unit certificate not trusted yet"* | Yangi head unit | Bildirishnomada yoki dasturda **Trust**ni bosing, qayta ulaning |
| Simsiz ulanish o‘z-o‘zidan boshlanmaydi | Telefon dasturni orqa fonda to‘xtatgan | [3-bo‘lim](#3-birinchi-sozlash)dagi Xiaomi sozlamalarini qiling. Yoki *Connect wirelessly*ni bosing |
| Tasvir qotadi, sifati past | Wi-Fi signali zaif | Telefonni head unit’ga yaqinlashtiring. *Connection → Maximum resolution*ni 720p qiling |
| Telefon ilovalari ochilmaydi | Shizuku ishlamayapti | Shizuku’ni qayta ishga tushiring ([7-bo‘lim](#7-telefon-ilovalari-avtomobil-ekranida)) |
| Wi-Fi sozlamalarida avtomobil tarmog‘iga parol so‘raladi | Bu tarmoqqa qo‘lda ulanish shart emas | Parol kiritmang: Alesle Avto o‘zi ulanadi |

**Batafsil jurnal (log):** Alesle Avto → **Advanced** → **Connection log**. Muammo haqida
yozganda shu jurnalni nusxalab yuboring.

---

## 11. Texnik ma’lumot va litsenziya

- Dasturchilar uchun to‘liq texnik hujjat (ingliz tilida): [docs/TECHNICAL.md](docs/TECHNICAL.md) —
  nimalar tayyor, yig‘ish (build), kompyuterda sinash (desktop head unit), mos keluvchi head
  unit’lar, kod tuzilishi.
- Sertifikatlar va head unit’lar: [docs/certificates.md](docs/certificates.md).

**Litsenziya:** GPL-3.0-or-later ([LICENSE](LICENSE)).

**Mualliflik:** Alesle Avto is based on AARE by prototux (github.com/prototux/AARE),
GPL-3.0-or-later. Not affiliated with Google. Android Auto is a trademark of Google LLC.
