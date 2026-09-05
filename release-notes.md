# 🇺🇸 English

**v0.2.5 — Modern Xposed API 102, seamless dynamic RTL/LTR switching & About page scroll fix**

## ✨ What’s new in v0.2.5
- **libxposed Modern Xposed API 102 support**: upgraded runtime libraries to `102.0.0` with `targetApiVersion=102` and `minApiVersion=101` in `module.prop`, ensuring compatibility with the newest Xposed specifications while maintaining full backward compatibility with API 101 frameworks.
- **Seamless dynamic RTL / LTR layout switching**: changing the app language in Settings (English / 中文 / العربية) immediately flips the layout direction (RTL ↔ LTR) without requiring a manual app kill and relaunch.
- **About page scroll & overflow fix**: resolved the issue where expanding the collapsible **Contributors** tree and individual breakdown cards caused content to overflow the screen without being able to scroll. Added vertical scrollbars, bottom navigation clearance padding, and automatic scroll-into-view on expansion.
- **Settings language selector**: polished Material 3 exposed dropdown (`TextInputLayout` + `MaterialAutoCompleteTextView`).

## Cumulative highlights (since v0.2.0)
- Built on the **libxposed Modern Xposed API (API 101/102)** — no legacy XposedBridge.
- **Multi-vendor Wi-Fi support** (AOSP, Samsung, Xiaomi, MediaTek, Huawei…), dynamic hotspot-interface detection, vendor factory-MAC reading.
- **Zero-click instant apply** — toggles and MAC edits sync to STA/AP interfaces immediately.
- **Arabic & RTL** UI, Material 3 design, remote preferences, XposedService activation detection.

## ⚠️ Install notes
- Root + LSPosed / Modern Xposed framework supporting API ≥ 101 (targeting API 102).
- Enable the module with scope **system** (system framework) and reboot.
- Package / app ID: `io.github.Rillwyn.androidmaceditor`.

## 🔗 Links
- Main repository: https://github.com/Rillwyn/android-mac-editor
- Xposed Modules Repo mirror: https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- Docs: README (EN/CN/AR) & CHANGELOG in the main repository.
- Based on [MAC Editor](https://github.com/jqssun/android-mac-editor) by [jqssun](https://github.com/jqssun), AGPL-3.0.

---

# 🇨🇳 中文

**v0.2.5 —— Modern Xposed API 102、动态 RTL/LTR 切换与关于页滚动修复**

## ✨ v0.2.5 更新内容
- **libxposed Modern Xposed API 102 支持**：依赖升级至 `102.0.0`，在 `module.prop` 中声明 `targetApiVersion=102` 与 `minApiVersion=101`，对接最新现代 Xposed 规范，同时完美向后兼容 API 101 框架。
- **无缝动态 RTL / LTR 布局切换**：在设置页切换语言（English / 中文 / العربية）后，界面布局方向（RTL ↔ LTR）立即生效，无需手动强行结束并重开应用。
- **关于页滚动与溢出修复**：彻底修复在关于页点开“贡献者”列表与版本详情时内容超出屏幕但无法滚动的问题。新增纵向滚动条、底部导航栏防遮挡内边距，并在点开时自动平滑居中显示。
- **设置页语言选择器**：Material 3 风格下拉框（`TextInputLayout` + `MaterialAutoCompleteTextView`）。

## 累积亮点（自 v0.2.0）
- 基于 **libxposed Modern Xposed API（API 101/102）**，不再依赖 legacy XposedBridge。
- **多厂商 Wi-Fi 支持**（AOSP、Samsung、Xiaomi、MediaTek、Huawei 等）、动态热点接口识别、厂商出厂 MAC 读取。
- **零点击即时生效**——开关/MAC 变更立即同步到 STA/AP 接口。
- **阿拉伯语与 RTL** 界面、Material 3 设计、Remote Preferences、XposedService 激活检测。

## ⚠️ 安装说明
- 需要 Root 并安装支持 Modern Xposed API（API ≥ 101，针对 API 102）的 LSPosed 等框架。
- 在 LSPosed 中启用模块并将作用域设为 **system（系统框架）**，然后重启设备。
- 包名/应用 ID：`io.github.Rillwyn.androidmaceditor`。

## 🔗 相关链接
- 主仓库：https://github.com/Rillwyn/android-mac-editor
- Xposed 模块镜像仓库：https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- 文档见主仓库 README（EN/CN/AR）与 CHANGELOG。
- 基于 [MAC Editor](https://github.com/jqssun/android-mac-editor)（作者 [jqssun](https://github.com/jqssun)），AGPL-3.0。

---

# 🇸🇦 العربية

**v0.2.5 — دعم Xposed API 102 الحديث، تبديل فوري لاتجاه RTL/LTR، وإصلاح تمرير صفحة حول**

## ✨ ما الجديد في v0.2.5
- **دعم libxposed Modern Xposed API 102**: ترقية الاعتماديات إلى `102.0.0` مع ضبط `targetApiVersion=102` و `minApiVersion=101` في `module.prop` لدعم أحدث معايير إطار Xposed مع الحفاظ على التوافقية الكاملة مع أطر API 101 السابقة.
- **تبديل فوري وديناميكي لاتجاه RTL / LTR**: عند تغيير اللغة في الإعدادات (English / 中文 / العربية) ينعكس اتجاه الواجهة فورًا (RTL ↔ LTR) دون الحاجة لإغلاق التطبيق وإعادة فتحه يدويًا.
- **إصلاح التمرير وتجاوز الشاشة في صفحة حول**: حل مشكلة عدم إمكانية التمرير عند فتح شجرة **المساهمون** وبطاقات تفاصيل الإصدارات، مع إضافة أشرطة تمرير رأسية ومسافة أمان تمنع حجب المحتوى خلف شريط التنقل السفلي، والتمرير التلقائي السلس عند فتح القوائم.
- **قائمة اللغات في الإعدادات**: قائمة منسدلة أنيقة بتصميم Material 3 (`TextInputLayout` + `MaterialAutoCompleteTextView`).

## أبرز المميزات التراكمية (منذ v0.2.0)
- مبني على **libxposed Modern Xposed API (API 101/102)** — دون أي اعتماد على XposedBridge القديم.
- **دعم واسع لشبكات Wi-Fi لمختلف الشركات المصنعة** (AOSP و Samsung و Xiaomi و MediaTek و Huawei وغيرها)، واكتشاف ديناميكي لواجهات نقطة الاتصال، وقراءة MAC المصنعي للشركات.
- **تطبيق فوري بدون نقرة** — تتم مزامنة المفاتيح وتعديلات MAC مع واجهات STA/AP فورًا.
- **دعم كامل للغة العربية واتجاه RTL**، تصميم Material 3، تفضيلات بعيدة (Remote Preferences)، وكشف تفعيل عبر XposedService.

## ⚠️ ملاحظات التثبيت
- يتطلب Root مع تثبيت LSPosed يدعم Modern Xposed API (الإصدار ≥ 101، مستهدفًا API 102).
- تفعيل الوحدة بنطاق **system** (إطار النظام) وإعادة تشغيل الجهاز.
- اسم الحزمة / معرّف التطبيق: `io.github.Rillwyn.androidmaceditor`.

## 🔗 الروابط
- المستودع الرئيسي: https://github.com/Rillwyn/android-mac-editor
- مرآة مستودع وحدات Xposed: https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- الوثائق: README (EN/CN/AR) وسجل التغييرات CHANGELOG في المستودع الرئيسي.
- مستند على [MAC Editor](https://github.com/jqssun/android-mac-editor) للمطوّر [jqssun](https://github.com/jqssun)، بترخيص AGPL-3.0.
