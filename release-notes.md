# 🇨🇳 中文

**v0.2.0 —— 全面迁移至 libxposed Modern Xposed API（API 101）+ 全新 UI 打磨**

## ✨ 本次更新亮点
- **现代 Xposed API（API 101）**：模块入口改为 `io.github.libxposed.api.XposedModule`（`META-INF/xposed/java_init.list`），`module.prop` 声明 `minApiVersion=101 / targetApiVersion=101`，作用域 `system`（system_server）。替代旧 YukiHookAPI / XposedBridge（api-82）方案。
- **Remote Preferences**：设置由框架数据库跨进程实时同步到 system_server Hook（替代 XSharedPreferences），改设置无需重启、即时生效。
- **激活检测（XposedService）**：以框架是否向 App 推送服务判断激活，重启后状态立即正确；状态卡新增框架/API/作用域/远程偏好通道诊断与“作用域缺 system”告警。
- **Hook 重写为拦截链模型**：`WifiNative` / `WifiVendorHal` 的 `setSta/setApMacAddress` 覆写、`Resources.getBoolean` 强制随机化支持位、构造器缓存等全部迁移，并兼容 ColorOS/OPPO 等厂商出厂 MAC 读取路径。
- **UI / 体验打磨**：状态卡小字统一并新增等宽 MAC 小字；MAC 输入框改为 Material3 OutlinedBox（内置清空按钮，保留自动格式化）；输入时软键盘不再顶起底部导航（adjustPan）。
- **图标**：状态图标换用官方 Material Symbols（CheckCircle / Error / Warning）。
- **包名/应用 ID** 统一为 `io.github.Rillwyn.androidmaceditor`。

## ⚠️ 安装与升级注意
- 需 Root 设备并安装**支持 libxposed Modern Xposed API（API ≥ 101）的 LSPosed**。
- 在 LSPosed 中启用模块并把作用域设为 **system（系统框架）**，然后**重启设备**。
- 若从旧版（YukiHookAPI 时代）升级：因包名改变，请先卸载旧包再安装本版。

## 🔗 相关链接
- 主仓库（源码 / Issue / Release）：https://github.com/Rillwyn/android-mac-editor
- Xposed 模块镜像仓库：https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- 使用文档 / 更新日志：见主仓库 README 与 CHANGELOG
- 本模块基于 [MAC Editor](https://github.com/jqssun/android-mac-editor)（作者 [jqssun](https://github.com/jqssun)），遵循 AGPL-3.0 许可

---

# 🇺🇸 English

**v0.2.0 — Full migration to the libxposed Modern Xposed API (API 101) + UI polish**

## ✨ Highlights
- **Modern Xposed API (API 101)**: the entry is now an `io.github.libxposed.api.XposedModule` (`META-INF/xposed/java_init.list`); `module.prop` declares `minApiVersion=101 / targetApiVersion=101` with scope `system` (system_server). The legacy YukiHookAPI / XposedBridge (api-82) stack is gone.
- **Remote Preferences**: settings are synced from the framework database to the `system_server` hooks in real time (replaces XSharedPreferences) — no reboot needed after changing a toggle.
- **Activation detection via XposedService**: the app is considered active once the framework pushes its service; the status card now also shows framework/API/scope/remote-preferences diagnostics plus a “scope missing `system`” warning.
- **Hooks rewritten on the interceptor-chain model**: `setSta/setApMacAddress` overrides on `WifiNative` / `WifiVendorHal`, forced MAC-randomization support bits via `Resources.getBoolean`, constructor caching — all migrated, with vendor (ColorOS/OPPO etc.) factory-MAC read paths preserved.
- **UI polish**: unified smaller status text with dedicated monospace MAC lines; the MAC input is a Material3 OutlinedBox with a built-in clear button (auto-formatting kept); the soft keyboard no longer pushes the bottom navigation up (adjustPan).
- **Icons**: status icons now use official Material Symbols (CheckCircle / Error / Warning).
- **Package / application ID** unified to `io.github.Rillwyn.androidmaceditor`.

## ⚠️ Installation & Upgrade Notes
- Requires a rooted device with an **LSPosed build that supports the libxposed Modern Xposed API (API ≥ 101)**.
- Enable the module in LSPosed and set its scope to **system (system framework)**, then **reboot**.
- Upgrading from the old YukiHookAPI-era build: uninstall the previous package first (package ID changed).

## 🔗 Links
- Main repository (source / issues / releases): https://github.com/Rillwyn/android-mac-editor
- Xposed Modules Repo mirror: https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor
- Usage docs / changelog: see README and CHANGELOG in the main repository
- Based on [MAC Editor](https://github.com/jqssun/android-mac-editor) by [jqssun](https://github.com/jqssun), licensed under AGPL-3.0
