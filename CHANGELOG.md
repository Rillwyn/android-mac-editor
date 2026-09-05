# Changelog

This file documents all modifications made to the original [MAC Editor](https://github.com/jqssun/android-mac-editor) project by [jqssun](https://github.com/jqssun).

All changes are made under the terms of the original license (AGPL-3.0), and all original copyright notices are retained.

---

## [0.2.5] - 2026-09-05

### Added & Changed
- **libxposed Modern Xposed API 102 support**: upgraded `libxposed:api` and `libxposed:service` to `102.0.0`; configured `targetApiVersion=102` and `minApiVersion=101` in `module.prop` to support the newest modern framework specifications while maintaining backward compatibility with API 101 environments.
- **Dynamic RTL / LTR layout switching**: changing the app language in Settings (English / 中文 / العربية) immediately flips the layout direction (RTL ↔ LTR) across the entire UI without requiring a manual app kill and relaunch.
- **About page scroll & overflow fix**: resolved the issue where expanding the collapsible **Contributors** tree and individual breakdown cards caused content to overflow the screen without being able to scroll. Added vertical scrollbars, bottom navigation clearance padding (`clipToPadding="false"` + `paddingBottom="96dp"`), and automatic scroll-into-view on expansion.
- Version bumped to `0.2.5` (`versionCode` 17); `module.prop` synced.

---

## [0.2.2] - 2026-09-05

### Changed (UI)
- **Settings — language dropdown**: the three-button language selector was replaced with a Material 3 exposed-dropdown (`TextInputLayout` + `MaterialAutoCompleteTextView`): English / 中文 / العربية.
- **About — maintainer & contributors**: the maintainer card now shows **Rillwyn** as repository maintainer; a new collapsible **Contributors** card lists **Rillwyn** and **Eng. Amr Eldeeb**, each with a per-version breakdown of what they did (EN / 中文 / العربية).
- Version bumped to `0.2.2` (`versionCode` 13 → 14); `module.prop` synced.

---

## [0.2.1] - 2026-09-05

### Merged (community PR #1 by [engamreldeeb](https://github.com/engamreldeeb))
- **Multi-vendor compatibility**: the hooker now probes AOSP + OEM `WifiNative` / `WifiVendorHal` stacks (Samsung `Sem*`, Xiaomi `Miui*`, MediaTek `Mtk*`, Huawei `Hw*`, …), watches `ServiceManager.addService("wifi")` for late-loading class loaders, detects dynamic hotspot interfaces (`ap*`, `softap*`, `swlan*`, `wlanN`) and reads vendor factory-MAC storage (Samsung EFS, Qualcomm `wlan_mac.bin`, …) — all ported onto the libxposed API 101 engine.
- **Zero-click instant apply**: UI switch/MAC changes now broadcast `ACTION_CONFIG_CHANGED`; `system_server` applies the custom MAC to the STA and (optionally) AP interfaces immediately — no manual “Apply” / hotspot toggle needed.
- **Arabic & RTL**: full `values-ar` translation, RTL layout support (`android:supportsRtl`), LTR protection for hex MAC fields, and a third language selector (English / 中文 / العربية) in Settings.
- **Co-maintainer credit**: author/About updated to **Rillwyn & Eng. Amr Eldeeb**.

### Changed
- Version bumped to `0.2.1` (`versionCode` 12 → 13); `module.prop` synced.
- Docs updated: README (EN/CN/AR), CHANGELOG (EN/CN/AR), release notes.

---

## [0.2.0] - 2026-09-05

### Core refactor: migrated to the libxposed Modern Xposed API (API 101)
- **Modern module entry**: Removed the YukiHookAPI entry (`HookEntry` / `@InjectYukiHookWithXposed` / KSP processor / `assets/xposed_init`); added `MacEditorModule : XposedModule`, declared in `META-INF/xposed/java_init.list`.
- **Build dependency swap**: `io.github.libxposed:api:101.0.1` (compileOnly) + `io.github.libxposed:service:101.0.0` (implementation) replace `com.highcapable.yukihookapi:*` and the local `libs/api-82.jar`; KSP removed. `module.prop` declares `minApiVersion=101` / `targetApiVersion=101` / `staticScope=true`; scope `scope.list` = `system` (system_server).
- **Manifest cleanup**: Removed the legacy `xposedmodule` / `xposedminversion` / `xposedscope` meta-data and the `MODULE_SETTINGS` category. Module name/description now come from `android:label` / `android:description`.

### Changed
- **Cross-process preferences now use Remote Preferences (framework database)**: the module app reads/writes via `XposedService.getRemotePreferences`, the `system_server` hook side reads (read-only) via `XposedModule.getRemotePreferences` and registers a change listener — settings apply live without reboot. The local `SharedPreferences` file is only a cache when inactive and the source for a one-shot sync on activation.
- **Activation detection via XposedService**: `YukiHookAPI.Status.isModuleActive` → `App.isModuleActive()` (the framework only pushes its service to the module app while the module is active); the UI refreshes the status card through a service-state listener.
- **System-MAC pull over broadcast**: `YukiHookDataChannel` → the app sends `ACTION_QUERY_MAC` and `system_server` answers with `ACTION_MAC_DETECTED`.
- **Hook model switched to interceptor chains**: `Member.hook { before/after }` → `hook(Executable).intercept { chain -> … chain.proceed(newArgs) }`. `Resources.getBoolean` is still overridden via a plain method hook (the modern API removed resource replacement — this module does not need it).
- Build config: `compileSdk` 37 → 36 (YukiHookAPI's hard requirement on 37 is gone); removed `libs/api-82.jar`, `arrays.xml`, KSP, and `android.suppressUnsupportedCompileSdk`.
- Version bumped to `0.2.0` (`versionCode` 11 → 12; `module.prop` updated to match).

### Documentation
- Updated the YukiHookAPI / legacy XposedBridge architecture descriptions in `README.md`, `README_CN.md`.

### Follow-up additions
- **Status-card icons switched to official Material Symbols vectors**: active `CheckCircle`, inactive `Error`, hook-off `Warning` (replacing the hand-made `ic_baseline_router_24` / `ic_error_24` / `ic_warning_24`).
- **Extended status detection**: while active, the status card shows framework name/version, Xposed API version, scope and remote-preferences capability (from `XposedService`); added a dedicated “scope does not include `system`” warning branch plus actionable hints for the inactive / hook-off states; when the framework lacks remote-preferences capability (no `PROP_CAP_REMOTE`) or the service is briefly unavailable, preferences automatically fall back to the local cache instead of throwing.
- **Release signing**: generated `keystore/maceditor-release.p12` inside the repo (PKCS12, RSA-4096, git-ignored); parameters (`storeFile`/`storePassword`/`keyAlias`/`keyPassword`/`storeType`) are written to the ignored `local.properties`, so `assembleRelease` signs automatically (v2 signature verified).
- **Unified small text & smaller MAC lines in the status card**: subtitle and extra rows now both use `BodySmall`; MAC addresses moved to a dedicated smaller monospace row (`status_mac`, shown only while active).
- **IME behavior**: `MainActivity` now uses `windowSoftInputMode="adjustPan"` — while typing a MAC the window pans so the input stays visible, and the bottom navigation no longer rises to the top of the keyboard but stays at the screen bottom (covered by the keyboard).
- **Material-styled MAC input**: the “Standby MAC” input is now a Material3 `TextInputLayout` (OutlinedBox) with its section title turned into a floating hint, plus the built-in **clear_text** end icon. All original behaviour is kept (character filtering / auto-capitalization / auto-formatting / generate / apply).

---

## [0.1.0] - 2026-08-26

### Added
- **Three-page UI with bottom navigation + swipe**: The single scrolling page was restructured into **Home / Settings / About** pages, switched via `BottomNavigationView` + `ViewPager2` (tap a tab or swipe horizontally). Requires the new `androidx.viewpager2` dependency.
  - **Home**: module status card, "Override randomized MAC" switch, MAC address card (system MAC / active MAC / standby MAC input / generate / apply), and the footer note — the core MAC features stay on the home page.
  - **Settings**: inline language selection (English / 中文, saved immediately and rebuilt in place — the app returns to the page you were on), "Force enable MAC randomization" switch, and "Override AP MAC address" switch.
  - **About**: app icon, app name, version read automatically from `BuildConfig.VERSION_NAME`, this project and original project links (project name + full URL, whole row clickable, opens in browser), a source note ("Based on the original project's approach, fully rewritten with YukiHookAPI"), and the maintainer (Rillwyn).
- Toolbar title now follows the current page (Home shows the app name, Settings/About show their titles).
- Three new vector icons for the bottom navigation tabs (home / settings / info).

### Changed
- `MainActivity` is now a container hosting the `ViewPager2` adapter and bottom-navigation linkage; the language menu was removed from the toolbar (moved to the Settings page); the current tab is remembered so a language switch (`recreate()`) returns to the same page.
- New fragments `HomeFragment` / `SettingsFragment` / `AboutFragment` own the migrated UI logic; the broadcast receiver and DataChannel system-MAC pull live in `HomeFragment`.
- Version bumped to `0.1.0` (`versionCode` 10 → 11).

### Fixed
- **`META-INF/xposed/` files were missing from the release APK** (module.prop, scope.list): restored under `src/main/resources/META-INF/xposed/` so they are packaged into the APK root again — LSPosed needs them to recognize the module and its scope. `java_init.list` stays absent on purpose (it would switch LSPosed to the libxposed loading path and break the module).

### Documentation
- Updated `UI_REFACTOR_PLAN.md` (the agreed refactor plan for this change).

---

## [0.0.10] - 2026-08-25

### Core Refactor
- **Fully migrated to YukiHookAPI 1.3.2**: The module entry and hook implementations were rebuilt on official YukiHookAPI mechanics, fixing the previous source code that referenced non-existent APIs (`YukiModule`, `Preferences.default`, `module.encounter`, `module.injectResource`, etc.) and could not compile at all.
  - New entry `HookEntry` uses `@InjectYukiHookWithXposed` + `IYukiHookXposedInit`; the KSP processor auto-generates `assets/xposed_init` and the module-status class, so the hand-maintained `META-INF/xposed` assets were removed.

### Fixed
- **“Module inactive” shown after reboot**: Activation state now uses `YukiHookAPI.Status.isModuleActive` (LSPosed injects the real activation state into the module's own process), instead of relying on the transient “system MAC broadcast” signal. The app shows the correct status immediately after reboot, no background-clearing needed.
- **First “Apply MAC Address” tap did nothing after reboot**: The module now hooks every `WifiNative` constructor and caches the instance as soon as the system creates it; `WifiVendorHal` overloads are also hooked for compatibility. The native instance is always available when the user taps “Apply MAC Address”.
- **Broken cross-process preferences**: Module app and `system_server` now share preferences through YukiHookAPI's `YukiHookPrefsBridge` (read/write inside the module app, XSharedPreferences read-only in the host process), replacing the old custom “prefs bridge + broadcast” scheme. `customMac`, `hookActive`, `apMacOverride` are shared in real time.
- **Preference file readability**: Enabled `isEnableHookSharedPreferences` to force the module preference file to mode 0664 (world-readable), so `system_server` can read it under any framework mode.

### Changed
- Build config: `compileSdk` raised to 37 (required by YukiHookAPI 1.3.2 dependencies); KSP (`ksp-xposed`) added; local `libs/api-82.jar` provides the XposedBridge compile-time API; the dead `maven.highcapable.me` repository was removed.
- **Resource hook now hooks `Resources.getBoolean`**: intercepts `getBoolean(int)` in `system_server` and returns true for the target resource names (plain method hook, compatible with frameworks like LSPosed that do not support XResources replacement; the toggle takes effect immediately, no reboot needed).
- **Hooks rewritten with `Member.hook`**: target members are resolved via reflection and hooked directly, avoiding the overload-resolution issue of the legacy finder style (`method { }.hook { }`) in some environments.
- **Double installation of hooks**: hooks are installed directly in `loadSystem` and again when the `WifiService` class is loaded (via `SystemServiceManager.loadClassFromLoader`), covering any loading timing.
- **Xposed assets & module declarations**: `module.prop` and `scope.list` restored under `src/main/resources/META-INF/xposed/` (packaged into the **APK root `META-INF/xposed/`**); the Manifest now carries the legacy Xposed module declarations (`MODULE_SETTINGS`, `xposedmodule`/`xposeddescription`/`xposedminversion`/`xposedscope`) so LSPosed recognizes the module reliably. The entry is still provided by the KSP-generated `assets/xposed_init`; `java_init.list` is deliberately **not** created — if present, LSPosed would switch to the libxposed loading path, which would make the module fail to load.
- **"System MAC" now shows the factory MAC**: Reflects `WifiVendorHal.getStaFactoryMacAddress(iface)` (ColorOS/OPPO method name; AOSP standard is `getFactoryMacAddress`, both are tried) to read the hardware factory MAC, instead of the randomized one. The app actively pulls it via `YukiHookDataChannel` on process start and caches it locally, so the UI shows it immediately.
- **One-tap "Apply MAC Address" works immediately**: The broadcast now carries the target MAC directly (no longer depends on cross-process prefs timing); if the `WifiNative` instance is not ready yet, it automatically retries with a delay.
- **Status display polish**: The status-card subtitle dynamically shows the MAC actually in use (custom MAC and system MAC on separate lines); "Active MAC" falls back to the system MAC when no custom MAC is set.

### Documentation
- Updated `README.md` and `README_CN.md`.
- Added `CODE_SUMMARY.md` code summary document.
- Bumped version to `0.0.10` in `app/build.gradle.kts`.

---

## [0.0.9] - 2026-08-24

### Fixed
- **Release build activation issue**: Corrected the package name and scope in `META-INF/xposed` files (`java_init.list`, `module.prop`, `scope.list`) to ensure LSPosed properly loads the module. The previous files still referenced the original author's package name (`io.github.jqssun.maceditor`) and used an invalid scope (`system` instead of `android`), preventing the module from being injected into the system server.

---

## [0.0.8] - 2026-08-24

### Added
- **Multi‑language UI**: Support for English and Chinese, switchable via the app menu. The UI refreshes instantly after selection.
- **AP MAC override toggle**: A dedicated switch to control whether the custom MAC address is applied to the mobile hotspot (AP) interface. **Default is OFF**, preventing hotspot startup failures on devices that reject MAC changes on `wlan2`.

### Changed
- **Hotspot compatibility fix**: Replaced method‑based detection with interface‑name‑based detection (`wlan2`) to accurately identify AP calls, ensuring the AP override switch works reliably.
- **Language storage optimization**: Language preference is now stored in local `SharedPreferences` instead of remote Xposed prefs, fixing the previous issue where language switching had no effect.

### Technical Details
- Modified `WifiServiceHooker.kt`: Uses `ifaceName` to determine if the call targets AP mode; checks `apMacOverride` preference before replacing MAC.
- Modified `MainActivity.kt`: Reads language setting in `attachBaseContext` from local prefs, uses `recreate()` to refresh UI.
- Added `values-zh/strings.xml` for Chinese strings.
- Updated `activity_main.xml`: Added AP MAC override card.
- Bumped version to `0.0.8` in `app/build.gradle.kts`.

### Documentation
- Updated `README.md` and added `README_CN.md` to describe new features and usage.
- Created `CHANGELOG.md` and `CHANGELOG_CN.md` to record all modifications.

---

## Original Releases (upstream)

The original project was based on [David Berdik](https://f-droid.org/repo/com.berdik.macsposed_6_src.tar.gz)'s `MacSposed` implementation, later improved and open‑sourced by [jqssun](https://github.com/jqssun).

Original features included:
- Hooking `setStaMacAddress` and `setApMacAddress`.
- Forcing MAC randomization resources.
- Basic UI for setting a custom MAC address.