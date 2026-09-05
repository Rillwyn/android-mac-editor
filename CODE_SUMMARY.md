# MAC Editor — 代码总结与架构文档

> **当前版本**：v0.2.5（libxposed Modern Xposed API 101/102）
> **项目包名**：`io.github.Rillwyn.androidmaceditor`
> **支持平台**：Android 12+，LSPosed 等现代 Xposed 框架（Modern Xposed API ≥ 101，针对 API 102）

本文档总结 **MAC Editor for Android** 当前架构、核心机制、跨进程通信模型及构建方式。

---

## 1. 项目概览

MAC Editor 是一款基于现代 libxposed API 构建的开源 Android Xposed 模块，用于精细控制 Wi-Fi MAC 地址：

- **手动覆写 MAC**：覆写客户端 Wi-Fi（STA）与移动热点（AP）的 MAC 地址；
- **零点击即时生效**：UI 切换开关或修改 MAC 立即通过广播触发 `system_server` 实时同步并应用，无需手动反复开关热点或重启；
- **多厂商 Wi-Fi 栈兼容**：自动探测 AOSP、Samsung（`Sem*`）、Xiaomi（`Miui*`）、MediaTek（`Mtk*`）、Huawei（`Hw*`）等厂商定制的 `WifiNative`/`WifiVendorHal` 栈，监听 `ServiceManager.addService("wifi")`，动态识别 `ap*` / `softap*` / `swlan*` / `wlanN` 等热点接口；
- **硬件出厂 MAC 获取**：反射调用厂商 HAL 读取真实物理出厂 MAC，不受系统随机化或模块覆写影响；
- **强制开启 MAC 随机化**：在 `system_server` 中拦截 `Resources.getBoolean`，强制开启系统隐藏的 MAC 随机化能力；
- **多语言与动态 RTL 切换**：内置 English、中文与 العربية 完整本地化，支持实时 Material 3 下拉切换与无需重启应用的动态 RTL ↔ LTR 布局方向即时切换；
- **关于页与贡献者树**：关于页展示维护者卡片与两级可折叠贡献者树（Rillwyn 与 Eng. Amr Eldeeb），支持纵向滚动、展开自动居中及防底部导航遮挡。

---

## 2. 技术栈与依赖

| 依赖库 / 工具 | 版本 | 用途 |
|---|---|---|
| `io.github.libxposed:api` | 102.0.0（compileOnly） | 现代 Xposed API 核心接口（运行时由框架注入） |
| `io.github.libxposed:service` | 102.0.0（implementation） | 模块 App 进程与 Xposed 框架通信（Remote Preferences / 激活检测 / 作用域） |
| `androidx.appcompat:appcompat` | 1.6.1 | 基础应用兼容库、全局语言与布局方向管理（`AppCompatDelegate`） |
| `com.google.android.material:material` | 1.12.0 | Material 3 设计组件（CardView、Switch、Exposed Dropdown 等） |
| `androidx.viewpager2:viewpager2` | 1.1.0 | 主页/设置/关于 三页面滑动容器 |
| AGP / Kotlin | 8.13.2 / 2.2.10 | 构建工具链 |
| compileSdk / minSdk / targetSdk | 37 / 29 / 36 | 编译与目标 Android SDK |

---

## 3. 系统架构与进程模型

```
┌─────────────────────────────────────────────────────────────────┐
│                    模块应用进程 (UI / App)                       │
│  MainActivity (ViewPager2 + BottomNavigationView)               │
│  ├── HomeFragment: 状态卡诊断、覆写开关、MAC 编辑器、即时应用     │
│  ├── SettingsFragment: 语言下拉框(动态RTL)、强制随机化、AP覆写   │
│  └── AboutFragment: 项目链接、维护者、可折叠贡献者树             │
│  App: XposedServiceHelper 监听绑定状态、本地缓存与远程偏好同步   │
└────────────────┬───────────────────────────────▲────────────────┘
                 │                               │
                 │ 1. ACTION_CONFIG_CHANGED /    │ 2. ACTION_MAC_DETECTED
                 │    ACTION_APPLY_MAC 广播      │    广播回传系统出厂 MAC
                 ▼                               │
┌────────────────────────────────────────────────┴────────────────┐
│                   system_server (系统服务宿主进程)               │
│  MacEditorModule (XposedModule 入口)                            │
│  ├── WifiServiceHooker:                                         │
│  │   ├── Hook WifiNative / WifiVendorHal (AOSP + 多厂商 OEM)    │
│  │   ├── 监听 ServiceManager.addService("wifi") 延迟加载        │
│  │   ├── 动态 AP 接口识别 (wlan2, ap*, softap*, swlan*, wlanN) │
│  │   ├── 读取出厂 MAC 存储 (Samsung EFS, Qualcomm wlan_mac.bin) │
│  │   └── 注册广播接收器执行即时零点击 MAC 应用                 │
│  └── WifiConfigHooker:                                          │
│      └── Hook Resources.getBoolean(int) 强制返回 true           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 核心文件与职责分配

| 文件路径 | 职责说明 |
|---|---|
| `MacEditorModule.kt` | 模块入口（继承 `XposedModule`）。在 `onSystemServerStarting` 中向 `system_server` 安装 `WifiServiceHooker` 与 `WifiConfigHooker`。 |
| `App.kt` | 应用程序入口（实现 `XposedServiceHelper.OnServiceListener`）。管理框架服务绑定状态、Remote Preferences 与本地偏好同步。 |
| `MainActivity.kt` | 容器 Activity。管理三页 Fragment 导航、工具栏联动、以及动态 RTL/LTR 布局方向控制（`attachBaseContext`、`applyOverrideConfiguration`、`decorView` 布局方向绑定）。 |
| `HomeFragment.kt` | 主页逻辑。状态卡实时诊断（框架名/版本/API/作用域）、MAC 地址管理、手动/自动生成 MAC、广播发送。 |
| `SettingsFragment.kt` | 设置页逻辑。Material 3 语言下拉选择（English / 中文 / العربية）、调用 `AppCompatDelegate.setApplicationLocales` 与平滑重启、强制随机化开关与 AP 覆写开关。 |
| `AboutFragment.kt` | 关于页逻辑。展示项目 URL、维护者信息、多层级可折叠贡献者树（点击展开自动平滑滚入可视区域）。 |
| `hookers/WifiServiceHooker.kt` | 核心 Hook 逻辑。多厂商 WifiNative/VendorHal 方法拦截、动态接口探测、零点击即时配置响应、出厂 MAC 探测。 |
| `hookers/WifiConfigHooker.kt` | 拦截 `Resources.getBoolean(int)`，针对 `config_wifi_*_mac_randomization_supported` 返回 true。 |
| `META-INF/xposed/module.prop` | 模块声明文件：`minApiVersion=101`、`targetApiVersion=102`、`staticScope=true`。 |
| `META-INF/xposed/java_init.list` | 声明现代入口类 `io.github.Rillwyn.androidmaceditor.MacEditorModule`。 |
| `META-INF/xposed/scope.list` | 声明模块作用域为 `system`（系统框架）。 |

---

## 5. 关键机制说明

### 5.1 现代 API 101/102 规范与元数据
- 模块入口完全基于 `io.github.libxposed`，不再依赖任何 Legacy XposedBridge 或 YukiHookAPI；
- `module.prop` 指定 `minApiVersion=101` 与 `targetApiVersion=102`，使模块既能运行于主流 API 101 框架（如 LSPosed 1.9+），又能充分兼容 API 102 现代规范（如 Vector 与新版框架）；
- `packaging` 配置将 `META-INF/xposed/*` 打包至 APK 根目录，供框架精确识别。

### 5.2 动态 RTL / LTR 实时切换机制
- 语言选择不仅更新文本资源的 `Locale`，更通过 `Configuration.setLayoutDirection(locale)` 正确计算 `layoutDirection`；
- 在 `MainActivity` 重写 `applyOverrideConfiguration` 并于 `onCreate` 直接指定 `window.decorView.layoutDirection` 与 `binding.root.layoutDirection`；
- 在设置中选择语言后，通过 `AppCompatDelegate.setApplicationLocales` 同步全局语言，并通过 `FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NO_ANIMATION` 无闪烁平滑重启，实现 **无需手动杀进程重开** 的即时布局方向切换。

### 5.3 关于页滚动与展开居中保障
- 解决嵌套滑动与屏幕溢出：`NestedScrollView` 配置 `fillViewport="true"`、`clipToPadding="false"` 及 `paddingBottom="96dp"`，彻底避免内容被浮动的 `BottomNavigationView` 遮挡；
- 开启纵向滚动条 `android:scrollbars="vertical"`；
- 点击展开贡献者列表或版本明细时，通过 `area.requestRectangleOnScreen` 自动向父级滑动容器请求滚动，将展开的内容完整平滑带入可视屏幕中。

### 5.4 零点击即时生效 (Instant Apply)
- 开关变动与 MAC 编辑完成时，模块发送标准广播 `ACTION_CONFIG_CHANGED`；
- `system_server` 侧的接收器立即取出最新配置并通过已缓存的 `WifiNative`/`WifiVendorHal` 实例调用 `setStaMacAddress` 与 `setApMacAddress`，无需用户每次手动点击“应用”按钮或开关热点。

---

## 6. 构建与发布

```bash
# 调试构建
./gradlew assembleDebug

# 发布构建（需要配置签名或使用 CI 自动构建）
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/release/app-release.apk`。
CI 工作流：推送 `v*` tag 自动触发 GitHub Actions 构建并发布 Release 产物及 Xposed Modules Repo 镜像。
