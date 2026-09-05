# MAC Editor for Android（修改版）

[English](README.md) | [中文](README_CN.md) | [العربية](README_AR.md)

[![release](https://img.shields.io/github/v/release/Rillwyn/android-mac-editor?style=flat&label=release&color=blue)](https://github.com/Rillwyn/android-mac-editor/releases)
[![build](https://img.shields.io/github/actions/workflow/status/Rillwyn/android-mac-editor/build-release.yml?style=flat&label=build)](https://github.com/Rillwyn/android-mac-editor/actions/workflows/build-release.yml)
[![downloads](https://img.shields.io/github/downloads/Rillwyn/android-mac-editor/total?style=flat&label=downloads)](https://github.com/Rillwyn/android-mac-editor/releases)
[![LSPosed 镜像](https://img.shields.io/github/downloads/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/total?style=flat&label=LSPosed%20镜像&logo=Android&labelColor=F48FB1&logoColor=ffffff)](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/releases)
[![license](https://img.shields.io/github/license/Rillwyn/android-mac-editor?color=green&style=flat)](https://github.com/Rillwyn/android-mac-editor/blob/main/LICENSE)

> **主仓库**：[github.com/Rillwyn/android-mac-editor](https://github.com/Rillwyn/android-mac-editor) —— 源码、Issue 与 Release
> **Xposed 模块镜像仓库**（仅同步 Release 与说明）：[github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor)
> Release 由主仓库的 `build-release.yml` 工作流自动构建发布（推送 `v*` tag 即触发）。

> **注意**：本项目是基于 [MAC Editor](https://github.com/jqssun/android-mac-editor) 原项目（作者 [jqssun](https://github.com/jqssun)）的修改版。所有原始版权归原作者所有。此版本增加了若干增强功能（详见下文）。当前由 **Rillwyn** 与 **Eng. Amr Eldeeb** 共同维护与扩展（社区 [PR #1](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/pull/1)，作者 [engamreldeeb](https://github.com/engamreldeeb)）。
> **语言**：English · 中文 · [العربية](README_AR.md) —— 界面支持 RTL 布局。

**MAC Editor** 是一款免费开源的 Xposed 模块，让您精细控制 Android 设备的 Wi-Fi MAC 地址。它支持手动覆写 MAC，并能强制开启 Android 原生 MAC 随机化功能（只要硬件支持，无需 OEM 额外实现）。

您可以用它来：
- 自定义 MAC 地址以增强隐私保护。
- 将随机 MAC 固定为您指定的值。
- 在厂商禁用 MAC 随机化的设备上强制开启该功能。
- **独立控制移动热点（AP）接口是否使用自定义 MAC** – 如果您的设备在修改热点 MAC 后无法开启热点，可关闭此开关（默认关闭）。

## 功能特性

- **手动覆写 MAC** – 设置任意有效的单播 MAC 地址（首字节必须为偶数）。
- **零点击即时生效（自 v0.2.1）** – 切换开关或修改 MAC 后立即同步到 Wi-Fi / 热点 HAL 与活跃接口，无需再手动点击“应用”。
- **多厂商兼容（自 v0.2.1）** – 自动探测 AOSP 与 OEM 定制 `WifiNative`/`WifiVendorHal`（Samsung、Xiaomi、MediaTek、Huawei 等）、动态 AP 接口识别与厂商出厂 MAC 读取。
- **强制 MAC 随机化** – 对标准 Wi-Fi、Wi‑Fi Direct 和移动热点启用隐藏的随机化支持。
- **按网络/按连接控制** – 在 Wi‑Fi 网络详情中勾选“使用随机 MAC”后生效。
- **AP MAC 覆写开关** – 独立开启/关闭对热点接口（`wlan2`）的 MAC 替换（默认 **关闭**）。可解决部分设备因修改热点 MAC 导致热点无法启动的问题。
- **多语言界面** – 支持 English、中文与 العربية（含 RTL 布局），可在**设置**页面随时切换。
- **三页面 UI（自 v0.1.0）** – **主页**（状态卡片、覆写随机 MAC 开关、MAC 地址卡片）、**设置**（语言、强制随机化、AP 覆写）、**关于**（项目链接、维护者、版本）。通过底部导航或左右滑动切换。

## 兼容性

- Android 12+（已测试至 Android 16 QPR2）
- 已 Root 设备，并安装支持 **libxposed Modern Xposed API（API ≥ 101）** 的 **LSPosed** 框架（自 v0.2.0 起，本模块不再使用 legacy XposedBridge 接口）

## 实现原理

本项目基于 **libxposed Modern Xposed API（API 101）** 实现（v0.2.0 起全面迁移，替代旧版 YukiHookAPI / XposedBridge 方案）。模块入口 `MacEditorModule` 继承 `io.github.libxposed.api.XposedModule`，在 `META-INF/xposed/java_init.list` 中声明；`module.prop` 声明 `minApiVersion=101` / `targetApiVersion=101`，作用域 `scope.list` = `system`（system_server）。

在较新 Android 版本中，Wi-Fi 子系统（通过 `WifiNative`）支持按网络或按连接随机化 MAC 地址。本模块钩住系统服务的以下方法，在随机化启用时手动指定 MAC：

- `WifiNative.setStaMacAddress()` / `WifiVendorHal.setStaMacAddress()` – 用于 Wi-Fi 客户端模式
- `WifiNative.setApMacAddress()` / `WifiVendorHal.setApMacAddress()` – 用于 Wi-Fi 接入点（热点）模式

同时，模块会强制系统认为 MAC 随机化受支持，通过在 **system_server 中 Hook `Resources.getBoolean(int)`**，按资源名拦截以下系统 bool 并返回 true：
- `config_wifi_connected_mac_randomization_supported`
- `config_wifi_p2p_mac_randomization_supported`
- `config_wifi_ap_mac_randomization_supported`

这对于硬件驱动支持随机化、但厂商未在软件中开启的设备尤其有用。此方式为普通方法 Hook，兼容 LSPosed 等不支持 XResources 资源替换的框架，且开关切换**即时生效**（无需重启）。

### 跨进程偏好设置（Remote Preferences）

模块应用与 system_server 之间通过 **Remote Preferences**（Xposed 框架数据库）共享偏好数据（现代 API 替代旧 XSharedPreferences 方案）：
- 模块应用内：通过 `XposedService.getRemotePreferences()` 可读可写（框架在模块激活时向 App 推送服务）；
- system_server（宿主进程）内：Hook 侧通过 `XposedModule.getRemotePreferences()` 只读同一份数据，并注册变更监听。

因此应用里设置的 MAC、开关会**实时**作用于 Hook 逻辑（无需重启）；本地 `SharedPreferences` 仅作为未激活时的缓存。

### 激活状态检测（XposedService）

应用内通过 `App.isModuleActive()`（模块 App 是否收到框架推送的 `XposedService`）判断模块是否已在 LSPosed 中激活——现代 API 不再向模块自身进程注入 Hook，收到服务即表示模块处于激活环境，重启后进入应用**立即显示正确状态**。

### 出厂 MAC 获取（系统 MAC 显示）

模块反射 `WifiVendorHal.getStaFactoryMacAddress(iface)`（ColorOS/OPPO 方法名；AOSP 标准为 `getFactoryMacAddress`，已做多候选兼容）获取**硬件出厂 MAC**，不受 MAC 随机化与模块替换影响。应用打开界面时发送 `ACTION_QUERY_MAC` 广播，system_server 侧回发 `ACTION_MAC_DETECTED` 并缓存显示（无需等待 WiFi 广播）。

### 可靠的“应用 MAC”执行

- 模块 Hook 了 `WifiNative` 的**全部构造器**：系统一旦创建实例即被缓存；
- “应用 MAC 地址”点击时**广播直接携带目标 MAC**（不依赖跨进程 prefs 读取时序），`WifiNative` 实例未就绪时自动延迟重试 —— 重启后**第一次点击即可生效**；
- 状态卡片副标题会**动态显示当前实际使用的 MAC**（自定义 MAC 与系统 MAC 分行展示）。

### AP MAC 覆写开关说明

部分设备在修改热点 MAC 地址后无法正常启动热点（日志中会出现 `Could not set interface MAC address for wlan2` 错误）。为避免此问题，模块在 UI 中提供了 **“覆写 AP MAC 地址”** 开关（默认关闭）。当开关关闭时，模块不会拦截 `setApMacAddress` 调用，系统将使用默认随机 MAC 启动热点。

如果您需要为热点也使用自定义 MAC，只需打开此开关即可。

## 使用方法

1. 安装模块，并在 LSPosed 中激活（作用域：**系统框架**）。
2. 重启设备。
3. 打开 **MAC Editor** 应用。
4. 如需替换随机 MAC，开启 **“覆写随机 MAC”**。
5. 输入有效的 MAC 地址（例如 `02:00:00:00:00:01`），或点击 **“生成随机 MAC”**。
6. 点击 **“应用 MAC 地址”**。
7. 对于 Wi‑Fi 连接，请确保在网络的“隐私”设置中选中 **“使用随机 MAC”**。
8. 如需为热点使用自定义 MAC，请开启 **“覆写 AP MAC 地址”**（如果热点无法启动，建议保持关闭）。
9. 重新连接 Wi‑Fi 或重启热点以应用更改。

## 语言切换

应用支持英文和中文。切换方法：
- 打开**设置**页面（底部导航）。
- 点击**语言**下的 **English** 或 **中文**。
- 界面会立即刷新，并回到您之前所在的页面。

## 高通设备注意事项

某些高通芯片组的硬件支持可通过查看以下文件确认：
- `/vendor/etc/wifi/kiwi_v2/WCNSS_qcom_cfg.ini`
- `/vendor/firmware/wlan/qca_cld/WCNSS_qcom_cfg.ini`

对于不支持 MAC 随机化的老款高通设备，建议直接修改 `wlan_mac.bin` 或 `/sys/wifi/mac_addr`，而不是使用本模块。

## 致谢

本项目是 [MAC Editor](https://github.com/jqssun/android-mac-editor) 原项目（作者 [jqssun](https://github.com/jqssun)）的修改版。  
最初的系统服务钩子实现由 [David Berdik](https://f-droid.org/repo/com.berdik.macsposed_6_src.tar.gz) 提供。  
感谢原作者的出色工作。

## AI 辅助开发

本项目使用 AI 辅助开发。

## 许可证

本项目基于 [GNU Affero General Public License v3.0](LICENSE) 开源（fork 自 [MAC Editor](https://github.com/jqssun/android-mac-editor)）。所有原始版权声明均已保留。