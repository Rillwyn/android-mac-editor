# MAC Editor لنظام Android (نسخة معدلة)

[English](README.md) | [中文](README_CN.md) | [العربية](README_AR.md)

[![release](https://img.shields.io/github/v/release/Rillwyn/android-mac-editor?style=flat&label=release&color=blue)](https://github.com/Rillwyn/android-mac-editor/releases)
[![build](https://img.shields.io/github/actions/workflow/status/Rillwyn/android-mac-editor/build-release.yml?style=flat&label=build)](https://github.com/Rillwyn/android-mac-editor/actions/workflows/build-release.yml)
[![downloads](https://img.shields.io/github/downloads/Rillwyn/android-mac-editor/total?style=flat&label=downloads)](https://github.com/Rillwyn/android-mac-editor/releases)
[![مرآة LSPosed](https://img.shields.io/github/downloads/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/total?style=flat&label=LSPosed%20mirror&logo=Android&labelColor=F48FB1&logoColor=ffffff)](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/releases)
[![الترخيص](https://img.shields.io/github/license/Rillwyn/android-mac-editor?color=green&style=flat)](https://github.com/Rillwyn/android-mac-editor/blob/main/LICENSE)

> **المستودع الرئيسي**: [github.com/Rillwyn/android-mac-editor](https://github.com/Rillwyn/android-mac-editor) — الشفرة المصدرية والقضايا والإصدارات (Releases).
> **مرآة مستودع وحدات Xposed** (الإصدارات والوصف فقط): [github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor)
> يتم بناء الإصدارات تلقائيًا بواسطة سير عمل `build-release.yml` في المستودع الرئيسي (عبر دفع وسم `v*`).

> **ملاحظة**: هذا المشروع عبارة عن نسخة معدلة (Fork) من مشروع [MAC Editor](https://github.com/jqssun/android-mac-editor) الأصلي للمطوّر [jqssun](https://github.com/jqssun). كامل التقدير وحقوق النشر تعود للمؤلف الأصلي. يضيف هذا الإصدار العديد من التحسينات (انظر أدناه). تتم صيانته وتطويره بواسطة **Rillwyn** (مسؤول المستودع) و **Eng. Amr Eldeeb** (مساهمة المجتمع [PR #1](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/pull/1) بواسطة [engamreldeeb](https://github.com/engamreldeeb)).
> **اللغات المدعومة**: English · 中文 · [العربية](README_AR.md) — تدعم الواجهة تخطيط الاتجاه من اليمين لليسار (RTL).

**MAC Editor** هي وحدة Xposed مجانية ومفتوحة المصدر تمنحك تحكمًا دقيقًا بعنوان MAC لشبكة Wi-Fi على أجهزة Android. تدعم الاستبدال اليدوي لعنوان MAC وتتيح تفعيل دعم عشوائية MAC الأصلي في Android على الأجهزة المدعومة بغض النظر عن تعديلات الشركات المصنعة (OEM).

يمكنك استخدامه لـ:
- تخصيص سلوك عنوان MAC لتعزيز الخصوصية.
- استبدال MAC العشوائي بقيمة ثابتة تحددها بنفسك.
- فرض تفعيل عشوائية MAC على الأجهزة التي قام المصنع بتعطيلها فيها برمجيًا.
- **التحكم المستقل في واجهة نقطة الاتصال المحمولة (AP)** — تعطيل استبدال MAC لنقطة الاتصال إذا كان جهازك يتعذر عليه بدء نقطة الاتصال عند تغيير عنوان MAC.

## المميزات

- **استبدال يدوي لعنوان MAC** – تعيين أي عنوان MAC أحادي الإرسال (unicast) صالح (يجب أن يكون البايت الأول زوجيًا).
- **تطبيق فوري (بدون نقرة) (منذ v0.2.1)** – يؤدي تبديل المفاتيح أو تعديل عنوان MAC إلى مزامنة القيمة فورًا مع طبقة HAL لواجهات Wi‑Fi ونقطة الاتصال والواجهات النشطة دون الحاجة للنقر اليدوي على "تطبيق".
- **توافقية واسعة مع مختلف الشركات (منذ v0.2.1)** – اكتشاف تلقائي لطبقات `WifiNative` / `WifiVendorHal` لدى AOSP والشركات المعدلة (Samsung و Xiaomi و MediaTek و Huawei وغيرها)، واكتشاف واجهات نقطة الاتصال الديناميكية وقراءة عنوان MAC المصنعي للشركات.
- **فرض عشوائية MAC** – تفعيل الدعم الخفي للعشوائية لشبكات Wi-Fi القياسية و Wi‑Fi Direct ونقطة الاتصال المحمولة.
- **تحكم مخصص لكل شبكة أو لكل اتصال** – يعمل عند تحديد «استخدام MAC عشوائي» في تفاصيل شبكة Wi‑Fi.
- **مفتاح مستقل لاستبدال MAC لنقطة الاتصال (AP)** – تمكين أو تعطيل استبدال MAC لواجهة نقطة الاتصال بشكل منفصل (افتراضيًا: **معطّل**). يفيد في الأجهزة التي تفشل فيها نقطة الاتصال عند تغيير عنوان MAC.
- **واجهة متعددة اللغات** – دعم اللغات English و 中文 و العربية (مع تبديل فوري وديناميكي لاتجاه RTL/LTR دون الحاجة لإعادة تشغيل التطبيق)، قابلة للتبديل من صفحة **الإعدادات** عبر قائمة منسدلة بتصميم Material 3.
- **واجهة بثلاث صفحات (منذ v0.1.0)** – **الرئيسية** (بطاقة الحالة، مفتاح استبدال MAC، بطاقة عنوان MAC)، **الإعدادات** (قائمة اللغات المنسدلة، فرض العشوائية، استبدال AP)، و **حول** (روابط المشروع، مسؤول المستودع، شجرة المساهمين مع التمرير السلس، الإصدار). التبديل عبر ألسنة التنقل السفلي أو بالسحب يمينًا ويسارًا.

## التوافق

- نظام Android 12 فأحدث (تم الاختبار حتى Android 16 QPR2).
- جهاز مروّت (Root) ومثبت عليه إطار **LSPosed** أو أي إطار Xposed حديث يدعم **libxposed Modern Xposed API (الإصدار ≥ 101، مستهدفًا API 102)** (منذ الإصدار v0.2.0 لم تعد هذه الوحدة تعتمد على واجهة XposedBridge القديمة).

## تفاصيل البناء والتنفيذ

بُنيت هذه الوحدة على **libxposed Modern Xposed API (API 101/102)** (تم الترحيل الكامل في v0.2.0، والترقية لدعم معايير API 102 في v0.2.5). ترث فئة المدخل `MacEditorModule` من `io.github.libxposed.api.XposedModule` ومُعلنة في `META-INF/xposed/java_init.list`؛ ويحدد `module.prop` الإصدار `minApiVersion=101` / `targetApiVersion=102` مع توجيه `scope.list` إلى `system` (خادم النظام system_server).

في إصدارات Android الحديثة، يدعم نظام Wi-Fi الفرعي (عبر `WifiNative`) عشوائية عناوين MAC لكل شبكة أو لكل اتصال. تقوم هذه الوحدة بخطف دوال خادم النظام التالية للسماح بالتعيين اليدوي عند تفعيل العشوائية:

- `WifiNative.setStaMacAddress()` / `WifiVendorHal.setStaMacAddress()` – لوضع العميل (STA) في Wi‑Fi.
- `WifiNative.setApMacAddress()` / `WifiVendorHal.setApMacAddress()` – لوضع نقطة الاتصال (AP / Hotspot).
- منذ **v0.2.1** يقوم الخطاف بفحص فئات `WifiNative`/`WifiVendorHal` الخاصة بمختلف الشركات المصنعة (`Sem*`، `Miui*`، `Mtk*`، `Hw*`…)، ومراقبة `ServiceManager.addService("wifi")`، واكتشاف واجهات AP الديناميكية (`ap*`، `softap*`، `swlan*`، `wlanN`)، وقراءة تخزين MAC المصنعي للشركات (Samsung EFS، Qualcomm `wlan_mac.bin`، وغيرها).
- تُطبّق التغييرات **فوريًا** (بدون نقرة): يرسل التطبيق بث `ACTION_CONFIG_CHANGED` ليقوم `system_server` بتطبيق عنوان MAC المخصص على واجهات STA و(اختياريًا) AP في الحال.

كما تجبر الوحدة النظام على اعتبار أن عشوائية MAC مدعومة عبر خطف **`Resources.getBoolean(int)` في `system_server`** وإرجاع قيمة true للموارد التالية:
- `config_wifi_connected_mac_randomization_supported`
- `config_wifi_p2p_mac_randomization_supported`
- `config_wifi_ap_mac_randomization_supported`

هذا مفيد للأجهزة التي يدعم عتادها وبرامج تشغيلها عشوائية MAC ولكن المصنّع لم يفعّلها برمجياً. ونظرًا لكونه خطاف دوال عادي، فهو متوافق مع كافة الأطر مثل LSPosed التي لا تدعم استبدال موارد XResources، ويسري التبديل **فورًا دون الحاجة لإعادة التشغيل**.

### التفضيلات عبر العمليات (Remote Preferences)

يتشارك تطبيق الوحدة مع `system_server` التفضيلات عبر **التفضيلات البعيدة (Remote Preferences)** (قاعدة بيانات إطار Xposed — البديل الحديث لـ XSharedPreferences):
- داخل تطبيق الوحدة: للقراءة والكتابة عبر `XposedService.getRemotePreferences()` (يدفع الإطار خدمته للتطبيق أثناء تفعيل الوحدة).
- داخل `system_server` (العملية المضيفة): للقراءة فقط عبر `XposedModule.getRemotePreferences()` مع تسجيل مستمع للتغييرات.

لذا فإن عنوان MAC والمفاتيح التي تضبطها في التطبيق تسري على منطق الخطاف **في الوقت الفعلي** (دون إعادة تشغيل). ويُستخدم ملف `SharedPreferences` المحلي فقط كذاكرة مؤقتة عندما تكون الوحدة غير نشطة.

### كشف حالة التفعيل (XposedService)

يحدد التطبيق تفعيل الوحدة عبر `App.isModuleActive()` — أي ما إذا كان تطبيق الوحدة قد استلم خدمة `XposedService` من الإطار. لم تعد الواجهة الحديثة تحقن خطافات داخل عملية التطبيق نفسه؛ ويعني استلام الخدمة أن الوحدة تعمل في بيئة مفعّلة، مما يعرض الحالة الصحيحة **فورًا بعد إعادة التشغيل**.

### استرجاع MAC المصنعي ("MAC النظام")

تستخدم الوحدة الانعكاس البرمجي لاستدعاء `WifiVendorHal.getStaFactoryMacAddress(iface)` (أو المعيار `getFactoryMacAddress` في AOSP) لقراءة **عنوان MAC المصنعي الفعلي للعتاد**، والذي لا يتأثر بالعشوائية أو بالاستبدال. عند فتح الواجهة، يرسل التطبيق بث `ACTION_QUERY_MAC` ويرد `system_server` بـ `ACTION_MAC_DETECTED` ليتم حفظه وعرضه فورًا.

### موثوقية تطبيق عنوان MAC

- تخطف الوحدة **كافة منشئات** `WifiNative`، ليتم تخزين الكائن بمجرد إنشائه من قِبل النظام.
- يؤدي النقر على "تطبيق عنوان MAC" إلى إرسال بث **يحمل عنوان MAC المستهدف مباشرة** (دون الاعتماد على توقيت قراءة التفضيلات عبر العمليات)؛ وإذا لم يكن كائن `WifiNative` جاهزًا بعد، تتم إعادة المحاولة تلقائيًا مع تأخير زمني بسيط — لذا فإن **أول نقرة بعد إعادة التشغيل تعمل فورًا**.
- يعرض العنوان الفرعي لبطاقة الحالة **ديناميكيًا عنوان MAC المستخدم فعليًا** (عرض MAC المخصص و MAC النظام في أسطر منفصلة).

### مفتاح استبدال MAC لنقطة الاتصال (AP)

تتعذر بعض الأجهزة عن بدء نقطة الاتصال المحمولة عند تعديل عنوان MAC (تظهر سجلات الأخطاء `Could not set interface MAC address for wlan2`). لتجنب ذلك، توفر الوحدة مفتاحًا مخصصًا في الواجهة **لتعطيل استبدال MAC لوضع AP** (افتراضيًا: معطّل). عندما يكون هذا المفتاح **معطلاً**، لن تعترض الوحدة استدعاءات `setApMacAddress`، مما يترك النظام يستخدم MAC العشوائي الافتراضي لنقطة الاتصال.

إذا كنت ترغب في استخدام MAC مخصص لنقطة الاتصال أيضًا، ما عليك سوى تفعيل هذا المفتاح.

## طريقة الاستخدام

1. قم بتثبيت الوحدة وتفعيلها في LSPosed (النطاق: **إطار النظام / System Framework**).
2. أعد تشغيل الجهاز.
3. افتح تطبيق **MAC Editor**.
4. فعّل **«استبدال MAC العشوائي»** إذا كنت ترغب في استبدال MAC العشوائي بآخر مخصص.
5. أدخل عنوان MAC صالحًا (مثال: `02:00:00:00:00:01`) أو اضغط على **«توليد MAC عشوائي»**.
6. اضغط على **«تطبيق عنوان MAC»** (أو اعتمد على التطبيق التلقائي الفوري).
7. بالنسبة لشبكات Wi‑Fi، تأكد من تحديد **«استخدام MAC عشوائي»** في إعدادات «الخصوصية» للشبكة.
8. لاستخدام MAC مخصص لنقطة الاتصال، فعّل **«استبدال MAC لنقطة الاتصال»** (يُفضل إبقاؤه معطلاً إذا فشل بدء نقطة الاتصال).
9. أعد الاتصال بشبكة Wi‑Fi أو أعد تشغيل نقطة الاتصال لتطبيق التغييرات.

## تبديل اللغة

يدعم التطبيق اللغات الإنجليزية والصينية والعربية. للتبديل:
- افتح صفحة **الإعدادات** (من شريط التنقل السفلي).
- اختر **English** أو **中文** أو **العربية** من القائمة المنسدلة لـ **اللغة**.
- تتحدث الواجهة وينعكس اتجاه التخطيط فورًا (RTL ↔ LTR) وتعود إلى نفس الصفحة التي كنت فيها دون الحاجة لإعادة تشغيل التطبيق.

## ملاحظات لأجهزة Qualcomm

يمكن التحقق من دعم العتاد في بعض معالجات Qualcomm بالاطلاع على:
- `/vendor/etc/wifi/kiwi_v2/WCNSS_qcom_cfg.ini`
- `/vendor/firmware/wlan/qca_cld/WCNSS_qcom_cfg.ini`

بالنسبة لأجهزة Qualcomm القديمة التي لا تدعم عشوائية MAC، يُفضّل تعديل `wlan_mac.bin` أو `/sys/wifi/mac_addr` مباشرة بدلاً من استخدام هذه الوحدة.

## شكر وتقدير

هذا المشروع مشتق من مشروع [MAC Editor](https://github.com/jqssun/android-mac-editor) الأصلي للمطوّر [jqssun](https://github.com/jqssun).  
تم توفير التنفيذ الأولي لخطف خدمات النظام بواسطة [David Berdik](https://f-droid.org/repo/com.berdik.macsposed_6_src.tar.gz).  
نتقدم بالشكر للمؤلفين الأصليين على عملهم الرائع.

## المساعدة بالذكاء الاصطناعي

تم تطوير هذا المشروع بمساعدة أدوات الذكاء الاصطناعي.

## الترخيص

هذا المشروع مشتق من [MAC Editor](https://github.com/jqssun/android-mac-editor) ومرخص تحت رخصة [GNU Affero General Public License v3.0](LICENSE). جميع إشعارات حقوق النشر الأصلية محفوظة.
