# 🇺🇸 English

**v0.2.2 — UI polish: language dropdown & About page contributor tree**

## ✨ What’s new in v0.2.2
- **Settings**: the language selector is now a Material 3 dropdown (English / 中文 / العربية) instead of three buttons.
- **About**: shows **Rillwyn** as the repository maintainer, plus a collapsible **Contributors** card listing **Rillwyn** and **Eng. Amr Eldeeb** — each name expands to show what they did per version (EN / 中文 / العربية).

## Cumulative highlights (since v0.2.0)
- Built on the **libxposed Modern Xposed API (API 101)** — no legacy XposedBridge.
- **Multi-vendor Wi-Fi support** (AOSP, Samsung, Xiaomi, MediaTek, Huawei…), dynamic hotspot-interface detection, vendor factory-MAC reading.
- **Zero-click instant apply** — toggles and MAC edits sync to STA/AP interfaces immediately.
- **Arabic & RTL** UI, Material 3 design, remote preferences, XposedService activation detection.

## ⚠️ Install notes
- Root + LSPosed supporting the Modern Xposed API (API ≥ 101).
- Enable the module with scope **system** (system framework) and reboot.
- Package / app ID: `io.github.Rillwyn.androidmaceditor`.

## 🔗 Links
- Main repository: https://github.com/Rillwyn/android-mac-editor
- Xposed Modules Repo mirror: https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- Docs: README (EN/CN/AR) & CHANGELOG in the main repository.
- Based on [MAC Editor](https://github.com/jqssun/android-mac-editor) by [jqssun](https://github.com/jqssun), AGPL-3.0.

---

# 🇨🇳 中文

**v0.2.2 —— 界面打磨：语言下拉框与关于页贡献者树**

## ✨ v0.2.2 更新内容
- **设置页**：语言选择由三个按钮改为 Material 3 下拉框（English / 中文 / العربية）。
- **关于页**：仓库维护者显示为 **Rillwyn**；新增可折叠“贡献者”卡，列出 **Rillwyn** 与 **Eng. Amr Eldeeb**——点开每个名字可查看各自在对应版本做了什么（中/英/阿三语）。

## 累积亮点（自 v0.2.0）
- 基于 **libxposed Modern Xposed API（API 101）**，不再依赖 legacy XposedBridge。
- **多厂商 Wi-Fi 支持**（AOSP、Samsung、Xiaomi、MediaTek、Huawei 等）、动态热点接口识别、厂商出厂 MAC 读取。
- **零点击即时生效**——开关/MAC 变更立即同步到 STA/AP 接口。
- **阿拉伯语与 RTL** 界面、Material 3 设计、Remote Preferences、XposedService 激活检测。

## ⚠️ 安装说明
- 需要 Root 并安装支持 Modern Xposed API（API ≥ 101）的 LSPosed。
- 在 LSPosed 中启用模块并将作用域设为 **system（系统框架）**，然后重启设备。
- 包名/应用 ID：`io.github.Rillwyn.androidmaceditor`。

## 🔗 相关链接
- 主仓库：https://github.com/Rillwyn/android-mac-editor
- Xposed 模块镜像仓库：https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- 文档见主仓库 README（EN/CN/AR）与 CHANGELOG。
- 基于 [MAC Editor](https://github.com/jqssun/android-mac-editor)（作者 [jqssun](https://github.com/jqssun)），AGPL-3.0。
