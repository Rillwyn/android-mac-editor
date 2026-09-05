# سجل التغييرات

يوثّق هذا الملف التعديلات على مشروع [MAC Editor](https://github.com/jqssun/android-mac-editor) الأصلي للمطوّر [jqssun](https://github.com/jqssun). جميع التعديلات بترخيص AGPL-3.0 مع الإبقاء على حقوق النشر الأصلية.

## [0.2.1] - 2026-09-05

- دمج مساهمة المجتمع [PR #1](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/pull/1) (بواسطة [engamreldeeb](https://github.com/engamreldeeb)) على قاعدة libxposed API 101:
  - توافق متعدد الشركات (Samsung `Sem*`، Xiaomi `Miui*`، MediaTek `Mtk*`، Huawei `Hw*`…) مع مراقبة `ServiceManager.addService("wifi")` واكتشاف واجهات نقطة الاتصال الديناميكية وقراءة MAC المصنعي.
  - تطبيق فوري بدون نقرة عبر بث `ACTION_CONFIG_CHANGED`.
  - ترجمة عربية كاملة ودعم RTL ومبدّل ثلاثي اللغات (English / 中文 / العربية).
  - الصيانة المشتركة: **Rillwyn & Eng. Amr Eldeeb**.
- رفع الإصدار إلى `0.2.1` (versionCode 13).

## [0.2.0] - 2026-09-05

- إعادة كتابة شاملة على libxposed Modern Xposed API (API 101): مدخل `MacEditorModule`، تفضيلات بعيدة (Remote Preferences)، كشف التفعيل عبر XposedService، وواجهة Material 3 محدّثة.

## [0.1.0] - 2026-08-26

- واجهة ثلاثية الصفحات مع شريط تنقّل سفلي وتبديل بالتمرير.
