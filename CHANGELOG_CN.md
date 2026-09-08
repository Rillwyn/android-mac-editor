# 修改记录（中文版）

本文件记录了基于 [MAC Editor](https://github.com/jqssun/android-mac-editor) 原项目（作者 [jqssun](https://github.com/jqssun)）所做的所有修改。

所有修改均遵循原项目许可证（AGPL-3.0），并保留了原始版权声明。

---

## [0.2.5] - 2026-09-05

### 新增与变更
- **libxposed Modern Xposed API 102 支持**：依赖升级至 `102.0.0`；在 `module.prop` 中配置 `targetApiVersion=102` 与 `minApiVersion=101`，对接最新现代框架规范，同时保持对 API 101 环境的全面向后兼容。
- **动态 RTL / LTR 布局切换**：在设置中切换语言（English / 中文 / العربية）后，全局界面布局方向（RTL ↔ LTR）立即无缝刷新，无需手动杀掉并重新打开应用。
- **关于页滚动与溢出修复**：彻底解决在关于页点开可折叠“贡献者”列表及各个版本明细卡片时内容超出屏幕但无法滚动的问题。新增纵向滚动条、底部导航栏防遮挡间距（`clipToPadding="false"` + `paddingBottom="96dp"`）以及展开时的平滑自动滚入可视区域。
- 版本号升至 `0.2.5`（`versionCode` 17）；`module.prop` 同步。

---

## [0.2.2] - 2026-09-05

### 变更（界面）
- **设置页 —— 语言下拉框**：语言选择由三按钮改为 Material 3 下拉框（`TextInputLayout` + `MaterialAutoCompleteTextView`）：English / 中文 / العربية。
- **关于页 —— 维护者与贡献者**：维护者卡改为仓库维护者 **Rillwyn**；新增可折叠“贡献者”卡，列出 **Rillwyn** 与 **Eng. Amr Eldeeb**，每人可按版本展开查看各自做了什么（中/英/阿三语）。
- 版本号升至 `0.2.2`（`versionCode` 13 → 14）；`module.prop` 同步。

---

## [0.2.1] - 2026-09-05

### 合并（社区 [PR #1](https://github.com/Xposed-Modules-Repo/io.github.Rillwyn.android-mac-editor/pull/1)，作者 [engamreldeeb](https://github.com/engamreldeeb)）
- **多厂商兼容**：Hooker 现在会探测 AOSP 与 OEM 定制 `WifiNative`/`WifiVendorHal`（Samsung `Sem*`、Xiaomi `Miui*`、MediaTek `Mtk*`、Huawei `Hw*` 等），监听 `ServiceManager.addService("wifi")` 捕获延迟加载的 ClassLoader，动态识别热点接口（`ap*`、`softap*`、`swlan*`、`wlanN`），并读取厂商出厂 MAC 存储（三星 EFS、高通 `wlan_mac.bin` 等）——以上全部移植到 libxposed API 101 引擎上。
- **零点击即时生效**：界面开关/MAC 变更即广播 `ACTION_CONFIG_CHANGED`，system_server 立即把自定义 MAC 应用到 STA 与（可选）AP 接口——无需手动“应用”或开关热点。
- **阿拉伯语与 RTL**：完整 `values-ar` 翻译、RTL 布局支持（`android:supportsRtl`）、MAC 十六进制字段强制 LTR，设置页新增三语切换（English / 中文 / العربية）。
- **共同署名**：作者/关于页更新为 **Rillwyn & Eng. Amr Eldeeb**。

### 变更
- 版本号升至 `0.2.1`（`versionCode` 12 → 13）；`module.prop` 同步。
- 文档更新：README（EN/CN/AR）、CHANGELOG（EN/CN/AR）、发布说明。

---

## [0.2.0] - 2026-09-05

### 核心重构：迁移至 libxposed Modern Xposed API（API 101）
- **模块入口改为现代 API**：删除 YukiHookAPI 入口（`HookEntry` / `@InjectYukiHookWithXposed` / KSP 处理器 / `assets/xposed_init`），新增 `MacEditorModule : XposedModule`，在 `META-INF/xposed/java_init.list` 声明。
- **构建依赖替换**：以 `io.github.libxposed:api:101.0.1`（compileOnly）与 `io.github.libxposed:service:101.0.0`（implementation）替换 `com.highcapable.yukihookapi:*` 与本地 `libs/api-82.jar`；移除 KSP 插件；`module.prop` 声明 `minApiVersion=101` / `targetApiVersion=101` / `staticScope=true`，作用域 `scope.list` = `system`（system_server）。
- **Manifest 清理**：移除 legacy 的 `xposedmodule` / `xposedminversion` / `xposedscope` meta-data 与 `MODULE_SETTINGS` category；模块名称/说明改由 `android:label` / `android:description` 提供。

### Changed
- **跨进程偏好设置改用 Remote Preferences（框架数据库）**：模块 App 经 `XposedService.getRemotePreferences` 读写，system_server 内 Hook 经 `XposedModule.getRemotePreferences` 只读并注册变更监听——设置修改后无需重启即时生效；本地 `SharedPreferences` 仅作未激活时缓存与“先设置后激活”的同步来源。
- **激活状态检测改用 XposedService 绑定**：`YukiHookAPI.Status.isModuleActive` → `App.isModuleActive()`（框架仅在模块激活时向模块 App 推送服务），UI 通过服务状态监听器实时刷新状态卡。
- **系统 MAC 拉取改广播**：`YukiHookDataChannel` → 应用发送 `ACTION_QUERY_MAC`，system_server 接收后回发 `ACTION_MAC_DETECTED`。
- **Hook 模型改为拦截链**：`Member.hook { before/after }` → `hook(Executable).intercept { chain -> … chain.proceed(新参数) }`；`Resources.getBoolean` 仍以普通方法 Hook 覆写返回值（现代 API 已移除资源替换能力，本模块不需要）。
- 构建配置：`compileSdk` 37 → 36（移除 YukiHookAPI 对 37 的依赖）；删除 `libs/api-82.jar`、`arrays.xml`、KSP 与 `android.suppressUnsupportedCompileSdk`。
- 版本号升至 `0.2.0`（`versionCode` 11 → 12，`module.prop` 同步更新）。

### 文档
- 更新 `README.md`、`README_CN.md` 中与 YukiHookAPI / legacy XposedBridge 相关的架构描述。

### 后续增补
- **状态卡图标换用官方 Material Symbols 矢量**：激活 `CheckCircle`、未激活 `Error`、Hook 关闭 `Warning`（替换原自绘 `ic_baseline_router_24` / `ic_error_24` / `ic_warning_24`）。
- **状态检测扩展**：激活时状态卡追加框架名/版本、Xposed API 版本、作用域与远程偏好通道能力（来自 `XposedService`）；新增“作用域未包含 system”告警分支与未激活/关闭时的操作提示；框架缺少远程偏好能力（无 `PROP_CAP_REMOTE`）或服务瞬断时自动回退本地缓存，不再抛异常。
- **release 签名**：生成仓库内 `keystore/maceditor-release.p12`（PKCS12，RSA-4096，已加入 `.gitignore`）；参数（`storeFile/storePassword/keyAlias/keyPassword/storeType`）写入已忽略的 `local.properties`，`assembleRelease` 自动签名（v2 已验证）。
- **状态卡小字统一与 MAC 小字**：副标题与扩展行统一为 `BodySmall`；MAC 地址移入独立更小的等宽小字行（`status_mac`，monospace，仅激活时显示）。
- **软键盘行为**：`MainActivity` 设置 `windowSoftInputMode="adjustPan"`——输入 MAC 时窗口整体平移以保证输入框可见，底部导航不再被顶到键盘上方，而是留在屏幕底部（被键盘覆盖）。
- **MAC 输入框 Material 化**：“待应用 MAC”输入框改为 Material3 `TextInputLayout`（OutlinedBox）风格，区块标题并入浮动 hint；新增 Material 内置 **clear_text 清空按钮**。原有功能全部保留（字符过滤 / 自动大写 / 自动格式化 / 生成 / 应用）。

---

## [0.1.0] - 2026-08-26

### 新增
- **三页面 UI：底部导航 + 滑动切换**：将原单页滚动布局重构为**主页 / 设置 / 关于**三个页面，通过 `BottomNavigationView` + `ViewPager2` 切换（点击底部 Tab 或左右滑动）。新增 `androidx.viewpager2` 依赖。
  - **主页**：模块状态卡片、「覆写随机 MAC」开关、MAC 地址卡片（系统 MAC / 当前 MAC / 待应用 MAC 输入框 / 生成随机 / 应用按钮）、底部说明文字——MAC 核心功能全部留在主页。
  - **设置**：语言行内单选（English / 中文，点击即保存并原地重建界面，重建后回到原所在页面）、「强制启用 MAC 随机化」开关、「覆写 AP MAC 地址」开关。
  - **关于**：应用图标、应用名、版本号（从 `BuildConfig.VERSION_NAME` 自动读取）、本项目与原项目链接（项目名 + 完整 URL，整行可点击并在浏览器打开）、来源说明（"Based on the original project's approach, fully rewritten with YukiHookAPI"）、当前维护者（Rillwyn）。
- 顶部工具栏标题随页面切换（主页显示应用名，设置/关于显示各自标题）。
- 新增三个底部导航 Tab 的矢量图标（主页 / 设置 / 关于）。

### 变更
- `MainActivity` 改为容器：承载 ViewPager2 适配器与底部导航联动；工具栏语言菜单移除（移入设置页）；记录当前所在页面，语言切换（`recreate()`）后回到同一页面。
- 新增 `HomeFragment` / `SettingsFragment` / `AboutFragment` 三个 Fragment 承载迁移后的 UI 逻辑；广播接收器与 DataChannel 系统 MAC 拉取逻辑位于 `HomeFragment`。
- 版本号升至 `0.1.0`（`versionCode` 10 → 11）。

### 修复
- **release APK 中丢失 `META-INF/xposed/` 文件**（module.prop、scope.list）：已恢复到 `src/main/resources/META-INF/xposed/`，重新打包进 APK 根目录——LSPosed 依赖这些文件识别模块与作用域。`java_init.list` 仍刻意不创建（存在会让 LSPosed 改走 libxposed 加载路径导致模块失效）。

### 文档
- 更新 `UI_REFACTOR_PLAN.md`（本次改动的总纲文档）。

---

## [0.0.10] - 2026-08-25

### 核心重构
- **全面迁移至 YukiHookAPI 1.3.2**：以 YukiHookAPI 官方机制重建模块入口与 Hook 实现，修复旧版代码引用了不存在的 API（`YukiModule`、`Preferences.default`、`module.encounter`、`module.injectResource` 等）导致源码无法编译的问题。
  - 新入口 `HookEntry` 使用 `@InjectYukiHookWithXposed` + `IYukiHookXposedInit`，由 KSP 处理器自动生成 `assets/xposed_init` 与模块状态检测类，不再手工维护 `META-INF/xposed` 资产。

### Fixed
- **重启后误显示“未激活”**：激活状态改用 `YukiHookAPI.Status.isModuleActive`（LSPosed 自动向模块自身进程注入激活状态），不再依赖“是否收到系统 MAC 广播”这种瞬态信号。重启后进入应用立即显示正确状态，无需清后台。
- **重启后第一次点击“应用 MAC”无反应**：新增 Hook `WifiNative` 全部构造器，系统一创建 WifiNative 即缓存实例；同时兼容 `WifiVendorHal` 上的同名方法。点击“应用 MAC”时实例必定可用，首次点击即可生效。
- **跨进程偏好设置断裂**：模块应用与 system_server 之间改用 YukiHookAPI 的 `YukiHookPrefsBridge`（模块内可读写、宿主内 XSharedPreferences 只读同一份数据），彻底替代旧的“自定义 prefs 桥 + 广播”方案，`customMac`、`hookActive`、`apMacOverride` 等设置实时共享。
- **偏好文件可读性**：启用 `isEnableHookSharedPreferences`，强制模块偏好文件权限为 0664（world-readable），确保 system_server 在任何框架模式下均可读取。

### Changed
- 构建配置：`compileSdk` 升至 37（YukiHookAPI 1.3.2 依赖要求）；引入 KSP（`ksp-xposed`）；以本地 `libs/api-82.jar` 提供 XposedBridge 编译期 API；移除已失效的 `maven.highcapable.me` 仓库。
- **资源钩子改为 Hook `Resources.getBoolean`**：在 system_server 中拦截 `getBoolean(int)` 并按资源名返回 true（普通方法 Hook，兼容 LSPosed 等不支持 XResources 资源替换的框架；开关切换即时生效，无需重启）。
- **Hook 改用 `Member.hook` 新写法**：直接反射目标成员再 Hook，避免旧 finder 写法（`method { }.hook { }`）在部分环境的重载解析异常。
- **Hook 安装双保险**：`loadSystem` 时直接 Hook + 监听 `SystemServiceManager.loadClassFromLoader`，在 WifiService 类加载时二次安装，确保任何加载时序下都能 hook 上。
- **Xposed 资产与模块声明**：`module.prop`、`scope.list` 恢复在 `src/main/resources/META-INF/xposed/`（打包到 **APK 根目录 `META-INF/xposed/`**）；Manifest 补充传统 Xposed 模块声明（`MODULE_SETTINGS`、`xposedmodule`/`xposeddescription`/`xposedminversion`/`xposedscope`），确保 LSPosed 正确识别。入口由 KSP 生成的 `assets/xposed_init` 提供，**不创建 `java_init.list`**（该文件存在会让 LSPosed 改走 libxposed 加载路径导致模块失效）。
- **“系统 MAC”显示出厂 MAC**：通过反射 `WifiVendorHal.getStaFactoryMacAddress(iface)`（ColorOS/OPPO 方法名；AOSP 标准为 `getFactoryMacAddress`，已做多候选兼容）获取**硬件出厂 MAC**，不再显示随机化后的 MAC；应用进程启动时经 `YukiHookDataChannel` 主动拉取并缓存，打开界面立即显示。
- **一次点击“应用 MAC 地址”立即生效**：广播直接携带目标 MAC（不再依赖跨进程 prefs 读取时序），`WifiNative` 实例未就绪时自动延迟重试。
- **状态展示优化**：状态卡片副标题动态显示当前实际使用的 MAC（自定义 MAC 与系统 MAC 分行展示）；未设置自定义 MAC 时“当前 MAC”显示系统 MAC。

### 文档
- 更新 `README.md`、`README_CN.md`。
- 新增 `CODE_SUMMARY.md` 代码总结文档。
- 版本号升级至 `0.0.10`（`app/build.gradle.kts`）。

---

## [0.0.9] - 2026-08-24

### Fixed
- **Release 版本激活问题**：修正 `META-INF/xposed` 目录下 `java_init.list`、`module.prop`、`scope.list` 中的包名和作用域名称，确保 LSPosed 正确加载模块。
---

## [0.0.8] - 2026-08-24

### 新增功能
- **多语言界面**：支持英文和中文，通过应用菜单切换，界面即时刷新。
- **AP MAC 覆写开关**：新增独立开关，控制是否对移动热点（AP）接口应用自定义 MAC 地址。默认**关闭**，避免部分设备因修改热点 MAC 导致热点启动失败。

### 改进
- **热点兼容性修复**：改用接口名称（`wlan2`）而非方法比较来识别 AP 调用，确保 AP 覆写开关准确生效。
- **语言存储优化**：将语言偏好存储在本地 `SharedPreferences`，与模块远程配置分离，解决了之前切换无效的问题。

### 技术细节
- 修改 `WifiServiceHooker.kt`：通过 `ifaceName` 判断是否为 AP 接口，依据 `apMacOverride` 开关决定是否替换 MAC。
- 修改 `MainActivity.kt`：在 `attachBaseContext` 中从本地偏好读取语言，使用 `recreate()` 刷新界面。
- 新增 `values-zh/strings.xml` 中文资源文件。
- 更新 `activity_main.xml`：添加 AP MAC 覆写卡片。
- 版本号升级至 `0.0.8`（`app/build.gradle.kts` 中 `versionName`）。

### 文档
- 更新 `README.md` 和新增 `README_CN.md`，说明新功能和用法。
- 创建 `CHANGELOG.md` 和 `CHANGELOG_CN.md` 记录所有修改。

---

## 原始版本（上游）

初始版本基于 [David Berdik](https://f-droid.org/repo/com.berdik.macsposed_6_src.tar.gz) 的 `MacSposed` 实现，由 [jqssun](https://github.com/jqssun) 完善并开源。

原始功能包括：
- 钩子 `setStaMacAddress` 和 `setApMacAddress`。
- 强制 MAC 随机化资源覆写。
- 基本 UI 设置 MAC 地址。