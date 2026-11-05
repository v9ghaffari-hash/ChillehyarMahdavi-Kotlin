چله‌یار مهدوی - نسخه بومی Kotlin (V37)

این پروژه یک اپلیکیشن بومی Android با Kotlin و Jetpack Compose است.

ساختار اصلی:
- app/src/main: کد و منابع برنامه
- app/build.gradle: تنظیمات ماژول اندروید
- build.gradle و settings.gradle: تنظیمات پروژه Gradle
- .github/workflows/build.yml: ساخت خودکار APK با GitHub Actions

ساخت APK در GitHub:
1) همه محتویات این پوشه را مستقیماً در ریشه مخزن GitHub قرار دهید؛ خود پوشه ChillehyarMahdaviV37 را به‌عنوان یک پوشه تو در تو بارگذاری نکنید.
2) مطمئن شوید مسیر .github/workflows/build.yml در ریشه مخزن قرار دارد.
3) از تب Actions، گردش کار Build Chillehyar Mahdavi V37 APK را اجرا کنید.
4) پس از موفقیت، فایل APK را از بخش Artifacts همان اجرا دریافت کنید.

نیازمندی‌های ساخت:
- JDK 17
- Android SDK Platform 36
- Android Build Tools 36.0.0
- Gradle 8.11.1

اصلاحات این بسته:
- اصلاح امضای onRequestPermissionsResult برای سازگاری با AndroidX Activity.
- اصلاح Workflow برای نصب بسته‌های SDK مشخص و جلوگیری از درخواست بسته منسوخ tools.
- حذف وابستگی Workflow به فایل ZIP پروژه؛ ساخت مستقیماً از فایل‌های مخزن انجام می‌شود.

شناسه برنامه: ir.chillehyar.mahdi.next
نسخه: 37.0-ui-analysis-notification-fix
