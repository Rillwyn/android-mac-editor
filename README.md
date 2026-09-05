# MAC Editor for Android (Fork)

[English](README.md) | [中文](README_CN.md) | [العربية](README_AR.md)

[![release](https://img.shields.io/github/v/release/Rillwyn/android-mac-editor?style=flat&label=release&color=blue)](https://github.com/Rillwyn/android-mac-editor/releases)
[![build](https://img.shields.io/github/actions/workflow/status/Rillwyn/android-mac-editor/build-release.yml?style=flat&label=build)](https://github.com/Rillwyn/android-mac-editor/actions/workflows/build-release.yml)
[![downloads](https://img.shields.io/github/downloads/Rillwyn/android-mac-editor/total?style=flat&label=downloads)](https://github.com/Rillwyn/android-mac-editor/releases)
[![LSPosed mirror](https://img.shields.io/github/downloads/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/total?style=flat&label=LSPosed%20mirror&logo=Android&labelColor=F48FB1&logoColor=ffffff)](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/releases)
[![license](https://img.shields.io/github/license/Rillwyn/android-mac-editor?color=green&style=flat)](https://github.com/Rillwyn/android-mac-editor/blob/main/LICENSE)

> **Home (main repository)**: [github.com/Rillwyn/android-mac-editor](https://github.com/Rillwyn/android-mac-editor) — source code, issues and Releases.
> **Xposed Modules Repo mirror** (Releases + description only): [github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor)
> Releases are built automatically by the `build-release.yml` workflow in the main repository (push a `v*` tag to trigger).

> **Note**: This is a modified fork of the original [MAC Editor](https://github.com/jqssun/android-mac-editor) by [jqssun](https://github.com/jqssun). All credits go to the original author. This version adds several enhancements (see below). Maintained by **Rillwyn** (repository maintainer) and **Eng. Amr Eldeeb** (community PR #1 by [engamreldeeb](https://github.com/engamreldeeb)).
> **Languages**: English · 中文 · [العربية](README_AR.md) — UI supports RTL layouts.

**MAC Editor** is a free and open-source Xposed module that gives you granular control over the Wi-Fi MAC address on Android devices. It supports manual MAC override and enables native MAC randomization support exposed by Android on supported hardware regardless of the OEM's implementation.

You can use it to:
- Customize MAC behavior for privacy.
- Override the randomized MAC with a fixed value.
- Force-enable MAC randomization on devices where the vendor disabled it.
- **Optionally control whether the mobile hotspot (AP) interface uses the custom MAC** – disable this if your device fails to start hotspot when MAC is overridden.

## Features

- **Manual MAC override** – set any valid unicast MAC address (first octet even).
- **Instant (zero-click) apply** – toggling switches or changing the MAC immediately syncs the value to the Wi‑Fi / hotspot HAL and active interfaces (since v0.2.1).
- **Multi-vendor support** – automatic discovery of AOSP plus OEM `WifiNative`/`WifiVendorHal` stacks (Samsung, Xiaomi, MediaTek, Huawei…), dynamic AP-interface detection and vendor factory-MAC reading (since v0.2.1).
- **Force MAC randomization** – enable hidden randomization support for standard Wi-Fi, Wi‑Fi Direct, and mobile hotspot.
- **Per‑network or per‑connection control** – works when “Use randomized MAC” is selected in Wi‑Fi network details.
- **AP MAC override toggle** – independently enable/disable MAC replacement for the hotspot interface (default: **off**). Helps devices where changing AP MAC breaks hotspot functionality.
- **Multi‑language UI** – English, 中文 and العربية (with dynamic live RTL/LTR layout switching), switchable from the **Settings** page via a Material 3 dropdown.
- **Three‑page UI (since v0.1.0)** – **Home** (status card, MAC override switch, MAC address card), **Settings** (language dropdown, force randomization, AP override), and **About** (project links, maintainer, collapsible contributor tree with auto-scrolling, version). Switch by bottom navigation tabs or swiping left/right.

## Compatibility

- Android 12+ (tested up to Android 16 QPR2)
- Rooted devices with an **LSPosed** or modern Xposed framework that supports the **libxposed Modern Xposed API (API ≥ 101, targeting API 102)** (since v0.2.0 this module no longer uses the legacy XposedBridge interface)

## Implementation Details

This module is built on the **libxposed Modern Xposed API (API 101/102)** (fully migrated in v0.2.0, updated for API 102 in v0.2.5). The entry class `MacEditorModule` extends `io.github.libxposed.api.XposedModule` and is declared in `META-INF/xposed/java_init.list`; `module.prop` declares `minApiVersion=101` / `targetApiVersion=102` and `scope.list` targets `system` (system_server).
On modern Android, the Wi-Fi subsystem (via `WifiNative`) can randomize MAC addresses per network or per connection. This module hooks the following system server methods to allow manual MAC assignment when randomization is enabled:

- `WifiNative.setStaMacAddress()` / `WifiVendorHal.setStaMacAddress()` – for station (client) Wi‑Fi.
- `WifiNative.setApMacAddress()` / `WifiVendorHal.setApMacAddress()` – for access point (hotspot) mode.
- Since **v0.2.1** the hooker also probes OEM `WifiNative`/`WifiVendorHal` classes (`Sem*`, `Miui*`, `Mtk*`, `Hw*`…), watches `ServiceManager.addService("wifi")`, discovers dynamic AP interfaces (`ap*`, `softap*`, `swlan*`, `wlanN`), and reads vendor factory-MAC storage (Samsung EFS, Qualcomm `wlan_mac.bin`, …).
- Setting changes are applied **instantly** (zero-click): the app sends `ACTION_CONFIG_CHANGED` and `system_server` applies the custom MAC to the STA and (optionally) AP interfaces right away.

The module also forces the system to believe that MAC randomization is supported by hooking **`Resources.getBoolean(int)` in `system_server`** and returning true for the following resource names:
- `config_wifi_connected_mac_randomization_supported`
- `config_wifi_p2p_mac_randomization_supported`
- `config_wifi_ap_mac_randomization_supported`

This is useful on devices where the hardware and chipset drivers do support MAC randomization, but the vendor did not enable it in software. Being a plain method hook, it works on frameworks such as LSPosed that do not support XResources replacement, and the toggle takes effect **immediately** (no reboot needed).

### Cross-process Preferences (Remote Preferences)

The module app and `system_server` share preferences through **Remote Preferences** (the Xposed framework database — the modern replacement for XSharedPreferences):
- Inside the module app: readable and writable via `XposedService.getRemotePreferences()` (the framework pushes its service to the module app while the module is active).
- Inside `system_server` (host process): read-only via `XposedModule.getRemotePreferences()`, with a registered change listener.

So the MAC address and switches you set in the app take effect in the hook logic **in real time** (no reboot). The local `SharedPreferences` file is only used as a cache when the module is inactive.

### Activation State Detection (XposedService)

The app determines whether the module is activated via `App.isModuleActive()` — whether the module app has received the framework's `XposedService`. The modern API no longer injects hooks into the module's own process; receiving the service means the module is running in an active environment, so the correct status is shown **immediately after reboot**.

### Factory MAC Retrieval ("System MAC")

The module reflects `WifiVendorHal.getStaFactoryMacAddress(iface)` (ColorOS/OPPO method name; the AOSP standard `getFactoryMacAddress` is also tried) to read the **hardware factory MAC**, which is not affected by MAC randomization or the module's replacement. When the UI opens, the app sends an `ACTION_QUERY_MAC` broadcast; `system_server` answers with `ACTION_MAC_DETECTED` and the value is cached for display (no need to wait for a Wi-Fi broadcast).

### Reliable "Apply MAC Address"

- The module hooks **every constructor** of `WifiNative`, so the instance is cached as soon as the system creates it.
- Tapping "Apply MAC Address" sends a broadcast that **carries the target MAC directly** (no longer dependent on cross-process prefs timing); if the `WifiNative` instance is not ready yet, it automatically retries with a delay — so **the first tap after reboot works immediately**.
- The status-card subtitle **dynamically shows the MAC actually in use** (custom MAC and system MAC on separate lines).

### AP MAC Override Switch

Some devices fail to start the mobile hotspot when the MAC address is modified (error logs show `Could not set interface MAC address for wlan2`). To avoid this, the module provides a dedicated switch in the UI to **disable MAC override for AP mode** (default: off). When this switch is **off**, the module will not intercept calls to `setApMacAddress`, letting the system use the default random MAC for the hotspot.

If you need a custom MAC for hotspot as well, simply turn this switch **on**.

## Usage

1. Install the module and activate it in LSPosed (scope: **System Framework**).
2. Reboot your device.
3. Open the **MAC Editor** app.
4. Enable **“Override randomized MAC”** if you want to replace the randomized MAC with a custom one.
5. Enter a valid MAC address (e.g., `02:00:00:00:00:01`) or tap **“Generate Random MAC”**.
6. Tap **“Apply MAC Address”**.
7. For Wi‑Fi connections, ensure **“Use randomized MAC”** is selected in the network’s “Privacy” setting.
8. To use the custom MAC for hotspot, enable **“Override AP MAC address”** (recommended to keep it off if hotspot fails to start).
9. Reconnect Wi‑Fi or restart hotspot to apply changes.

## Language Switching

The app supports English, Chinese, and Arabic. To switch:
- Open the **Settings** page (bottom navigation).
- Select **English**, **中文**, or **العربية** from the **Language** dropdown.
- The UI refreshes immediately (switching layout direction between RTL and LTR live) and returns to the page you were on without needing an app restart.

## Notes for Qualcomm Devices

Hardware support on certain chipsets can be checked by looking at:
- `/vendor/etc/wifi/kiwi_v2/WCNSS_qcom_cfg.ini`
- `/vendor/firmware/wlan/qca_cld/WCNSS_qcom_cfg.ini`

For legacy Qualcomm devices without MAC randomization support, consider editing `wlan_mac.bin` or `/sys/wifi/mac_addr` directly instead of using this module.

## Acknowledgements

This project is a fork of the original [MAC Editor](https://github.com/jqssun/android-mac-editor) by [jqssun](https://github.com/jqssun).  
The initial open‑source system server hook implementation was provided by [David Berdik](https://f-droid.org/repo/com.berdik.macsposed_6_src.tar.gz).  
We thank the original authors for their great work.

## AI Assistance

This project was developed with the assistance of AI tools.

## License

This project is a fork of [MAC Editor](https://github.com/jqssun/android-mac-editor) and is licensed under the [GNU Affero General Public License v3.0](LICENSE). All original copyright notices are retained.