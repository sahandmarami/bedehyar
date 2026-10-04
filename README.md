# حساب‌یار (HesabYar / Bedehyar)

اپلیکیشن اندرویدی مدیریت **بدهی، طلب، پرداخت و سررسید** — کاملاً فارسی، راست‌به‌چپ و آفلاین.

- زبان: **Kotlin** — رابط کاربری: **Jetpack Compose** + **Material 3**
- معماری: **MVVM** + Repository + StateFlow + Coroutines
- پایگاه داده: **Room** (نسخه ۲ با مهاجرت اصولی از نسخه ۱)
- تنظیمات: **DataStore**
- یادآوری: **AlarmManager** (`setAlarmClock`) + Foreground Service + صفحه آلارم تمام‌صفحه
- تقویم: **شمسی** با فونت **وزیرمتن** و اعداد فارسی
- حداقل اندروید: **8.0 (API 26)** — هدف: **API 34**
- بدون هیچ مجوز اینترنت؛ داده‌ها فقط روی دستگاه ذخیره می‌شوند.

## امکانات

- داشبورد: مجموع طلب‌ها/بدهی‌ها، خالص حساب، افراد با حساب باز، سررسیدهای امروز و گذشته، نزدیک‌ترین سررسید
- فهرست اشخاص با جست‌وجو، فیلتر (بدهکاران/طلبکاران/تسویه‌شده‌ها/سررسید گذشته) و مرتب‌سازی (نام/مبلغ/تاریخ)
- ثبت بدهی/طلب: نام شخص، مبلغ، نوع حساب، توضیحات، تاریخ ثبت و سررسید شمسی، ساعت یادآوری، دسته‌بندی، یادداشت خصوصی
- صفحه اختصاصی شخص: مانده، تاریخچه تراکنش‌ها و پرداخت‌ها، فیلتر تاریخ و وضعیت، ویرایش/حذف
- پرداخت کامل و جزئی با اعتبارسنجی (پرداخت هرگز از مانده بیشتر نمی‌شود) و بازمحاسبه خودکار
- آرشیو خودکار تراکنش‌های تسویه‌شده و لغو یادآوری پس از تسویه
- یادآوری دقیق سررسید: آلارم تمام‌صفحه با صدا و ویبره، «خاموش کردن» و «۱۰ دقیقه بعد» (تعویق بدون تغییر سررسید اصلی)
- بازسازی خودکار آلارم‌ها پس از ریبوت و به‌روزرسانی برنامه
- گزارش‌ها: بازه روزانه/هفتگی/ماهانه/دلخواه، نمودار دریافت/پرداخت، فیلتر شخص و دسته، خروجی CSV
- پشتیبان‌گیری/بازیابی JSON از طریق Storage Access Framework با اعتبارسنجی فایل
- تم روشن/تاریک/خودکار، واحد پول تومان/ریال

## ساخت پروژه

```bash
./gradlew assembleDebug          # APK دیباگ
./gradlew testDebugUnitTest      # تست‌های واحد
./gradlew assembleRelease        # APK ریلیز (بدون امضا اگر متغیرهای امضا ست نباشد)
```

خروجی‌ها: `app/build/outputs/apk/...`

### امضای نسخه Release

متغیرهای محیطی (هرگز در مخزن قرار نگیرند):

```
ANDROID_KEYSTORE_PATH       مسیر فایل keystore
ANDROID_KEYSTORE_PASSWORD   رمز keystore
ANDROID_KEY_ALIAS           نام کلید
ANDROID_KEY_PASSWORD        رمز کلید
```

اگر keystore ندارید، یکی تولید کنید:

```bash
keytool -genkeypair -v -keystore hesabyar-release.keystore \
  -alias hesabyar -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=HesabYar, O=HesabYar, L=Tehran, C=IR"
```

⚠️ فایل keystore را **هرگز** commit نکنید و نسخه پشتیبان امن از آن نگه دارید؛ بدون آن نمی‌توانید به‌روزرسانی امضاشده با همان کلید منتشر کنید.

## CI/CD

`.github/workflows/android-release.yml`:

- اجرای دستی (`workflow_dispatch`) و روی تگ‌های `v*`
- تست واحد → ساخت APK امضاشده → اعتبارسنجی `apksigner` → انتشار در GitHub Releases با `SHA256SUMS.txt`
- امضا از Secrets: `ANDROID_KEYSTORE_B64` (base64 keystore)، `ANDROID_KEYSTORE_PASSWORD`، `ANDROID_KEY_ALIAS`، `ANDROID_KEY_PASSWORD`
- اگر Secrets تنظیم نشده باشند، با یک کلید موقت CI امضا می‌شود و در Release هشدار می‌دهد.

تنظیم Secrets:

```bash
base64 -w0 hesabyar-release.keystore > ks.b64
gh secret set ANDROID_KEYSTORE_B64 < ks.b64
gh secret set ANDROID_KEYSTORE_PASSWORD
gh secret set ANDROID_KEY_ALIAS
gh secret set ANDROID_KEY_PASSWORD
```

## ساختار پروژه

```
app/src/main/java/ir/bedehyar/app/
├── App.kt                 # اپلیکیشن، کانال‌های اعلان، DI سبک
├── MainActivity.kt        # میزبان Compose، RTL، مجوزها و راهنمای اولیه
├── data/                  # Room (Entities/Daos/AppDatabase+Migration)، Ledger، Settings(DataStore)
├── alarm/                 # AlarmScheduler، AlarmReceiver، BootReceiver، AlarmService، AlarmActivity
├── ui/                    # تم، اجزای مشترک، تقویم شمسی، داشبورد، شخص، تراکنش، گزارش، تنظیمات
└── util/                  # Jalali (تبدیل تقویم)، Fmt، PaymentMath، Backup/CSV
```

## مهاجرت دیتابیس (v1 → v2)

نسخه ۱ فقط جدول تخت `transactions` داشت. نسخه ۲ داده‌ها را به `persons` / `transactions` / `payments` / `reminders` / `categories` نرمال‌سازی می‌کند: نام‌ها به Person، `paidAmount` به ردیف Payment و آلارم‌های معتبر به Reminder منتقل می‌شوند (بخش `MIGRATION_1_2` در `AppDatabase.kt`).
