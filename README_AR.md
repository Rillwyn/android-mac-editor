# MAC Editor for Android

[English](README.md) | [中文](README_CN.md) | [العربية](README_AR.md)

وحدة Xposed مفتوحة المصدر مبنية على **libxposed Modern Xposed API (API 101)** للتحكم الدقيق بعنوان MAC لشبكة Wi‑Fi ونقطة الاتصال المحمولة على أجهزة Android.

## المزايا
- **استبدال يدوي لعنوان MAC** مع فرض تفعيل دعم العشوائية المخفي في النظام.
- **تطبيق فوري (بدون نقرة إضافية)**: عند تغيير الإعدادات يُطبَّق العنوان مباشرة على واجهات STA/AP.
- **توافق متعدد الشركات**: اكتشاف تلقائي لطبقات `WifiNative` / `WifiVendorHal` لدى AOSP وSamsung وXiaomi وMediaTek وHuawei وغيرها، مع قراءة عنوان MAC المصنعي من ملفات الشركات.
- **دعم كامل للغة العربية وواجهات RTL** مع حماية حقول العنوان السداسي عشر باتجاه LTR.
- **واجهة Material 3** بثلاث صفحات (الرئيسية / الإعدادات / حول).

## التوافق
- Android 12+، مع LSPosed يدعم Modern Xposed API (الإصدار 101 فأعلى).
- تفعيل النطاق: **system** (إطار النظام) ثم إعادة تشغيل الجهاز.

## الروابط
- المستودع الرئيسي (الشفرة / القضايا / الإصدارات): https://github.com/Rillwyn/android-mac-editor
- مستودع وحدات Xposed (الإصدارات): https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- مستند على [MAC Editor](https://github.com/jqssun/android-mac-editor) للمطوّر [jqssun](https://github.com/jqssun)، بترخيص AGPL-3.0. الصيانة والتطوير: **Rillwyn** و **Eng. Amr Eldeeb**.
