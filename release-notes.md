# 🇺🇸 English

**v0.2.1 — Merge of community PR #1: multi-vendor support, zero-click apply, Arabic & RTL**

## ✨ Highlights
- **Multi-vendor compatibility**: automatic discovery of AOSP + OEM Wi-Fi stacks (Samsung `Sem*`, Xiaomi `Miui*`, MediaTek `Mtk*`, Huawei `Hw*`, …), `ServiceManager.addService("wifi")` watch, dynamic hotspot-interface detection (`ap*`, `softap*`, `swlan*`, `wlanN`) and vendor factory-MAC reading — all running on the **libxposed Modern Xposed API (API 101)** engine.
- **Zero-click instant apply**: toggling switches or editing the MAC immediately syncs it to the STA and (optionally) AP interfaces — no manual “Apply MAC” or hotspot reset needed.
- **Arabic & RTL**: full Arabic UI, RTL layout support, LTR-protected MAC fields, and a 3-way language switcher (English / 中文 / العربية).
- **Material 3 UI** polish (unified status text, monospace MAC lines, clearable MAC input, `adjustPan` soft-keyboard behavior, official Material status icons).
- Co-maintained by **Rillwyn & Eng. Amr Eldeeb**.

## ⚠️ Install notes
- Root + LSPosed supporting the Modern Xposed API (API ≥ 101).
- Enable the module with scope **system** (system framework) and reboot.
- Package / app ID: `io.github.Rillwyn.androidmaceditor`.

## 🔗 Links
- Main repository (source / issues / releases): https://github.com/Rillwyn/android-mac-editor
- Xposed Modules Repo mirror: https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- Docs: see README (EN/CN/AR) and CHANGELOG in the main repository.
- Based on [MAC Editor](https://github.com/jqssun/android-mac-editor) by [jqssun](https://github.com/jqssun), AGPL-3.0.

---

# 🇨🇳 中文

**v0.2.1 —— 合并社区 PR #1：多厂商支持、零点击即时生效、阿拉伯语与 RTL**

## ✨ 亮点
- **多厂商兼容**：自动探测 AOSP 与 OEM 定制 Wi‑Fi 栈（Samsung `Sem*`、Xiaomi `Miui*`、MediaTek `Mtk*`、Huawei `Hw*` 等），监听 `ServiceManager.addService("wifi")`，动态识别热点接口（`ap*`、`softap*`、`swlan*`、`wlanN`），读取厂商出厂 MAC —— 全部运行在 **libxposed Modern Xposed API（API 101）** 引擎上。
- **零点击即时生效**：切换开关或修改 MAC 后立即同步到 STA 与（可选）AP 接口，无需手动点击“应用 MAC”或重启热点。
- **阿拉伯语与 RTL**：完整阿拉伯语界面、RTL 布局支持、MAC 十六进制字段强制 LTR，设置页三语切换（English / 中文 / العربية）。
- **Material 3 UI** 打磨：状态小字统一、等宽 MAC 小字、可清空的 MAC 输入框、`adjustPan` 软键盘行为、官方 Material 状态图标。
- 共同维护：**Rillwyn & Eng. Amr Eldeeb**。

## ⚠️ 安装说明
- 需要 Root 并安装支持 Modern Xposed API（API ≥ 101）的 LSPosed。
- 在 LSPosed 中启用模块并将作用域设为 **system（系统框架）**，然后重启设备。
- 包名/应用 ID：`io.github.Rillwyn.androidmaceditor`。

## 🔗 相关链接
- 主仓库（源码 / Issue / Release）：https://github.com/Rillwyn/android-mac-editor
- Xposed 模块镜像仓库：https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- 文档见主仓库 README（EN/CN/AR）与 CHANGELOG。
- 基于 [MAC Editor](https://github.com/jqssun/android-mac-editor)（作者 [jqssun](https://github.com/jqssun)），AGPL-3.0。
