# MAC Editor — 代码总结（历史记录）

> ⚠️ **本文档记录的是 v0.0.10（YukiHookAPI / legacy XposedBridge 时期）的实现细节，属历史存档。**
> 当前源码已全面迁移至 **v0.2.0 + libxposed Modern Xposed API（API 101）**：入口改为 `MacEditorModule : XposedModule`（`META-INF/xposed/java_init.list`），偏好改为 Remote Preferences，激活检测改为 XposedService，包名统一为 `io.github.Rillwyn.androidmaceditor`。
> 请以 [README.md](README.md) / [README_CN.md](README_CN.md) 与 [CHANGELOG.md](CHANGELOG.md) 为准。

本文档总结 **MAC Editor for Android**（Xposed 模块）在 v0.0.10 完成 YukiHookAPI 重构后的代码结构、核心机制与构建方式。

## 1. 项目简介

MAC Editor 是一款基于 Xposed/LSPosed 的 Android 模块，用于精细控制 Wi-Fi MAC 地址：

- 手动覆写 Wi-Fi（STA）与热点（AP）的 MAC 地址；
- 强制开启系统隐藏的 MAC 随机化支持位（资源覆写）；
- 通过应用界面配置，system_server 中实时生效。

v0.0.10 之前，本项目处于“AI 半迁移”状态：源码引用了 YukiHookAPI 中**不存在**的 API（`YukiModule`、`Preferences.default`、`module.encounter`、`module.injectResource`、`module.logD` 等），导致项目**无法编译**，且存在三个运行时缺陷：

1. 重启后应用误显示“未激活”；
2. 重启后第一次点击“应用 MAC 地址”无效；
3. 跨进程偏好设置（system_server 读不到应用设置的 MAC）断裂。

v0.0.10 使用 **YukiHookAPI 1.3.2 的真实 API** 完全重写，上述问题均已修复。

## 2. 技术栈与依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| `com.highcapable.yukihookapi:api` | 1.3.2（AAR） | YukiHookAPI 核心库（XposedBridge/LSPosed 兼容层） |
| `com.highcapable.yukihookapi:ksp-xposed` | 1.3.2 | KSP 处理器：自动生成 `assets/xposed_init`、入口代理类、模块状态类 |
| `com.google.devtools.ksp` | 2.2.10-2.0.2 | Kotlin Symbol Processing |
| `de.robv.android.xposed:api`（本地 `libs/api-82.jar`） | 82 | XposedBridge 编译期 API（`compileOnly`，运行时由 LSPosed 提供） |
| AGP / Kotlin | 8.13.2 / 2.2.10 | 构建工具链 |
| compileSdk / minSdk / targetSdk | 37 / 29 / 36 | SDK 版本（YukiHookAPI 1.3.2 依赖要求 compileSdk ≥ 37） |

> `de.robv.android.xposed:api:82` 不在 Maven Central（原托管于已归档的 JCenter），因此以本地 jar 形式放入 `app/libs/`。

## 3. 模块架构（按进程）

```
┌─────────────────────────────┐        ┌──────────────────────────────────┐
│  模块应用进程（UI）           │        │  system_server（宿主进程）        │
│  MainActivity / PrefManager │        │  WifiServiceHooker（Hook 逻辑）   │
│  context.prefs()  可读写    │        │  param.prefs()   XSharedPrefs 只读│
│  YukiHookAPI.Status         │        │  Hook WifiNative / WifiVendorHal │
└─────────────┬───────────────┘        └───────────────┬──────────────────┘
              │  同一份偏好文件（io.github.Rillwyn.androidmaceditor.xml，0664）   │
              └───────────────────────────────────────────────────────────┘
┌─────────────────────────────┐
│  Zygote                     │
│  WifiConfigHooker           │
│  resources().hook → 资源替换│
└─────────────────────────────┘
```

- **模块应用进程**：UI 配置，向共享偏好写入 MAC/开关；通过 `YukiHookAPI.Status.isModuleActive` 读取真实激活状态；进程启动时经 `YukiHookDataChannel` 主动拉取系统 MAC。
- **system_server**：`loadSystem` 作用域内安装 WifiNative/WifiVendorHal 钩子（`Member.hook` 新写法 + WifiService 加载时二次安装），通过 XSharedPreferences **只读**共享偏好，替换 MAC 参数，响应 MAC 拉取请求。
- **资源钩子**：在 system_server 中 Hook `Resources.getBoolean`（强制 MAC 随机化支持位，开关即时生效）。

## 4. 核心文件说明

| 文件 | 职责 |
|---|---|
| `HookEntry.kt` | 模块入口（`@InjectYukiHookWithXposed` + `IYukiHookXposedInit`）。`onInit` 配置 YukiHookAPI（debug、强制偏好文件 0664）；`onHook` 在 `loadSystem` 中安装 WifiServiceHooker 与 WifiConfigHooker。 |
| `App.kt` | 自定义 Application（继承 ModuleApplication）：进程启动时通过 dataChannel 主动拉取系统 MAC 并写本地缓存。 |
| `hookers/WifiServiceHooker.kt` | system_server 侧核心 Hook：`Member.hook` 缓存 `WifiNative` 实例（构造器）、拦截 `setStaMacAddress`/`setApMacAddress`（含 `WifiVendorHal` 兼容）、WifiService 加载时二次安装、注册“应用 MAC”接收器（广播携带 MAC + 自动重试）、`getStaFactoryMacAddress` 出厂 MAC 获取、dataChannel 拉取响应。 |
| `hookers/WifiConfigHooker.kt` | 在 system_server 中 Hook `Resources.getBoolean(int)`，按资源名返回 true（强制 MAC 随机化）。 |
| `utils/PrefManager.kt` | 偏好设置访问入口，统一走 `context.prefs("io.github.Rillwyn.androidmaceditor")`（YukiHookPrefsBridge）。 |
| `utils/MacUtils.kt` | MAC 校验与随机生成（未变）。 |
| `utils/MacTextWatcher.kt` | 输入自动格式化（未变）。 |
| `MainActivity.kt` | 主界面：状态卡（激活检测 + 动态副标题）、开关、MAC 输入与“应用 MAC”广播发送、onResume 主动拉取刷新。 |
| `MacBroadcastReceiver.kt` | 接收 system_server 广播的系统 MAC，写本地缓存供 UI 展示（尽力而为）。 |
| `AndroidManifest.xml` | 指定 `App`（继承 ModuleApplication）；传统 Xposed 模块声明（`MODULE_SETTINGS`、`xposedmodule`/`xposeddescription`/`xposedminversion`/`xposedscope`）。 |

## 5. 关键机制详解

### 5.1 模块入口与 KSP 生成

```kotlin
@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {
    override fun onInit() { YukiHookAPI.configs { isDebug = BuildConfig.DEBUG; isEnableHookSharedPreferences = true } }
    override fun onHook() = encase {
        loadSystem {
            WifiServiceHooker.hook(this)
            WifiConfigHooker.hook(this)
        }
    }
}
```

KSP 处理器（`ksp-xposed`）自动生成：

- `assets/xposed_init` → 指向 `io.github.Rillwyn.androidmaceditor.HookEntry_YukiHookXposedInit`（LSPosed 据此加载模块）；
- 入口代理类（`IXposedHookZygoteInit` / `IXposedHookLoadPackage` / `IXposedHookInitPackageResources` 实现）与 `HookEntry_Impl`；
- `YukiXposedModuleStatus_Impl_Impl`：模块状态类。LSPosed 检测到该类后，会向模块自身进程**注入真实的激活状态**，供 `YukiHookAPI.Status.isModuleActive` 读取；
- `META-INF/yukihookapi_init`：模块入口标记（Java resources，打包到 APK 根 `META-INF/`）。

因此模块入口**不需要**手工维护 `META-INF/xposed/java_init.list`。此外，项目在 `src/main/resources/META-INF/xposed/` 放置了两个 LSPosed 元数据文件（`src/main/resources` 是 Java resources，会打包到 **APK 根目录的 `META-INF/`**，这正是 LSPosed 读取的位置；**不要**放到 `src/main/assets/` 下，那会变成 `assets/META-INF/xposed/`——那是 libxposed 新式模块的位置，LSPosed 不会从这里读 XposedBridge 模块的元数据）：

```
app-release.apk（解压后）
├── assets/
│   └── xposed_init              # KSP 生成：入口 io.github.Rillwyn.androidmaceditor.HookEntry_YukiHookXposedInit
└── META-INF/                    # APK 根（来自 src/main/resources，Java resources）
    ├── xposed/
    │   ├── module.prop          # minApiVersion=101 / targetApiVersion=101 / staticScope=true
    │   └── scope.list           # system（静态作用域声明）
    └── yukihookapi_init         # KSP 生成：模块入口标记
```

> ⚠️ **不要创建 `assets/META-INF/xposed/java_init.list`**：只要该文件存在且非空，LSPosed 就会判定模块为 libxposed（新式）模块并**只**走 libxposed 加载路径，而 YukiHookAPI 1.3.2 的入口实现的是 XposedBridge（`de.robv.android.xposed.*`）接口，无法通过 libxposed 加载，模块将直接失效。模块识别完全由 `assets/xposed_init` 负责。

### 5.2 跨进程偏好共享（YukiHookPrefsBridge）

```kotlin
// 模块应用内（可读写）
context.prefs("io.github.Rillwyn.androidmaceditor").edit { putString("customMac", mac) }
// system_server 内（只读，XSharedPreferences）
param.prefs("io.github.Rillwyn.androidmaceditor").getString("customMac", "")
```

- 同一文件名（`io.github.Rillwyn.androidmaceditor.xml`）兼容旧版本数据；
- 写入时 `isEnableHookSharedPreferences` 强制文件权限 0664，保证宿主可读；
- system_server 每次读取都会 `reload()`，设置变更**实时**生效，无需额外监听器。

### 5.3 激活状态检测

```kotlin
val moduleActive = YukiHookAPI.Status.isModuleActive
```

由 LSPosed 注入的真实状态，替代旧版“是否收到系统 MAC 广播”的瞬态判断。重启后立即准确；状态卡逻辑：

| 状态 | 显示 |
|---|---|
| 未激活（`isModuleActive == false`） | 未激活 |
| 已激活但开关关闭 | 已激活 / Hook 关闭 |
| 已激活且开关开启 | 已激活 / 服务运行中 |

### 5.4 MAC 覆写与“应用 MAC”

- **Hook 方式**：统一使用 `Member.hook`（YukiHookAPI 推荐的新写法）—— 反射 `getDeclaredMethod` / `declaredConstructors` 直接 Hook 目标成员，避免旧 finder 写法（`method { }.hook { }`）在部分环境的重载解析异常。
- **实例缓存**：Hook `WifiNative` 全部构造器，实例一创建即缓存。
- **参数替换**：拦截 `setStaMacAddress(String, MacAddress)` / `setApMacAddress(...)` 的 `before` 回调，读偏好后 `args(1).set(MacAddress.fromString(customMac))`；同时 Hook `WifiVendorHal` 同名方法兼容不同 Android 版本。
- **AP 覆写开关**：非 `wlan0` 的 AP 接口且未开启覆写时放行，避免热点无法启动。
- **Hook 安装双保险**：`loadSystem` 时直接 Hook + 监听 `SystemServiceManager.loadClassFromLoader`，WifiService 类加载时用其 ClassLoader 二次安装（任何加载时序都能 hook 上）。
- **“应用 MAC”链路**：应用点击 → 广播 `ACTION_APPLY_MAC`（**直接携带目标 MAC**，不依赖跨进程 prefs 读取时序）→ system_server 接收器 → 用缓存实例反射调用 `setStaMacAddress`；实例未就绪时**自动延迟重试**（约 8 秒）→ 首次点击立即生效。
- **MAC 展示**：`handleMacCall` 在替换前将系统原始 MAC 广播写回应用本地缓存（`deviceMac`）。

### 5.5 资源钩子（强制 MAC 随机化）

```kotlin
val method = Resources::class.java.getDeclaredMethod("getBoolean", Int::class.javaPrimitiveType)
method.hook {
    after {
        // 按资源名拦截：config_wifi_*_mac_randomization_supported → result = true
    }
}
```

在 **system_server 中 Hook `Resources.getBoolean(int)`**（普通方法 Hook，兼容 LSPosed 等不支持 XResources 资源替换的框架），开关切换**即时生效**（无需重启）。

### 5.6 出厂 MAC 获取与主动拉取（YukiHookDataChannel）

```kotlin
// system_server：响应应用请求，回复出厂 MAC
param.dataChannel.with {
    wait<String>("mac_request") { _ ->
        val mac = currentSystemMac()          // getStaFactoryMacAddress 优先
        put("mac_result", mac)
    }
}
// 模块应用（App.onCreate / MainActivity.onResume）：主动请求
dataChannel("android").with {
    wait<String>("mac_result") { mac -> /* 写本地缓存 deviceMac */ }
    put("mac_request", "true")
}
```

- **出厂 MAC**：反射 `WifiVendorHal.getStaFactoryMacAddress(iface)`（ColorOS/OPPO 方法名；AOSP 标准 `getFactoryMacAddress` 也已尝试；另有 wlan_mac.bin 解析、替换前捕获值等回退）—— 不受随机化与模块替换影响。
- **主动拉取**：应用进程启动（`App.onCreate`）即通过 `YukiHookDataChannel` 拉取并缓存，打开界面**立即显示系统 MAC**（不再依赖 WiFi 广播时机）。

## 6. 构建方式

```bash
# 依赖：JDK 21、Android SDK（platform 37、build-tools 36.0.0）
# local.properties 需指向本机 SDK：sdk.dir=...
./gradlew :app:assembleDebug        # 调试包
./gradlew :app:assembleRelease      # 发布包（需签名配置）
```

产物：`app/build/outputs/apk/<variant>/app-<variant>.apk`。

## 7. 验证情况（v0.0.10）

已在本机 Android 14 模拟器（AVD `Small_Phone`）与 **OnePlus 8T（ColorOS 14，APatch + Zygisk-LSPosed）真机** 完成：

- ✅ `assembleRelease` 构建成功（仅 YukiHookAPI 1.x Legacy API 弃用警告）；
- ✅ APK 内含 KSP 生成的 `assets/xposed_init` 与 Manifest 传统 Xposed 声明（`MODULE_SETTINGS` + `xposedmodule` 等 meta-data），LSPosed 正确识别且不会自动禁用；
- ✅ 模块注入 system_server：资源钩子（`Forced config_wifi_* to true`）、`WifiNative hooks installed`、`WifiNative instance cached`；
- ✅ **一次点击“应用 MAC 地址”立即生效**（`Directly applied MAC ... on wlan0`）；
- ✅ **“系统 MAC”显示出厂 MAC**（真机 `getStaFactoryMacAddress` 返回 `AC:5F:EA:4D:6F:55`，与 wlan_mac.bin 一致）；
- ✅ 打开应用经 dataChannel 主动拉取并缓存（`Replied system MAC ... via data channel`）；
- ✅ 状态卡片副标题动态显示当前实际使用的 MAC。

## 8. 已知限制与后续改进

- `setStaMacAddress`/`setApMacAddress` 以显式 `(String, MacAddress)` 签名匹配，个别厂商修改签名时该钩子会自动跳过，其他钩子不受影响；
- 出厂 MAC 获取依赖 `WifiVendorHal.getStaFactoryMacAddress`（ColorOS/OPPO）或 AOSP `getFactoryMacAddress`，个别厂商两者都缺失时回退到 wlan_mac.bin 解析或“替换前捕获值”；
- system_server → 应用的数据通信目前为“广播（请求/应用 MAC）+ dataChannel（主动拉取回复）”混用，后续可统一迁移到 `YukiHookDataChannel`。
