# 三页面 UI 重构改动总纲（v3 终稿）

> 状态：**终稿**，所有事项已与用户逐项确认。待用户最终过目同意后实施。

## 一、目标

将当前单页滚动布局重构为三页面，底部导航 + 滑动切换：

| 页面 | 内容 |
|---|---|
| **主页 Home** | 更改 MAC 地址的核心功能 |
| **设置 Settings** | 语言、强制随机化开关、AP 覆写开关 |
| **关于 About** | 项目地址、维护者、应用图标与版本 |

## 二、技术方案（已确认）

- **导航**：`MainActivity` 保留为容器 + **ViewPager2**（支持左右滑动）+ **BottomNavigationView**（底部 Tab 点击切换，与滑动联动）
  - **新增依赖**：`androidx.viewpager2:viewpager2`（滑动切换所需）
  - material 1.12.0 自带 BottomNavigationView
- **布局**：
  - `activity_main.xml` 重构为「AppBarLayout(MaterialToolbar) + ViewPager2 + BottomNavigationView」三层结构
  - 新增 `fragment_home.xml` / `fragment_settings.xml` / `fragment_about.xml`
  - 现有各卡片按归属拆入对应 Fragment
- **工具栏**：保留顶部 Toolbar，**标题随页面切换**（主页=MAC Editor、设置=Settings、关于=About）
- **主题**：保持 `Theme.Material3.DynamicColors.DayNight.NoActionBar` 不变
- **字符串**：所有新增文案同步加入 `values/strings.xml` 与 `values-zh/strings.xml`（双语）

## 三、版本（已确认）

- **versionName：0.1.0**（用户指定）
- **versionCode：10 → 11**（递增）

## 四、页面内容划分（已确认）

### 主页 Home
- **状态卡片**（模块激活状态 + 动态 MAC 副标题）——保留在主页顶部
- **覆写随机 MAC 开关**（`hook_switch`）——**保留在主页**（用户确认）
- **MAC 地址卡片**（系统 MAC / 当前 MAC / 待应用 MAC 输入框 / 生成随机 / 应用按钮）——保留
- **底部说明文字**（footer_note）——移到主页底部（用户确认）

### 设置页 Settings（语言置顶，用户确认）
- **语言设置**——行内单选（English / 中文），点击即保存并 `recreate()`，重建成后恢复原所在页面
- **强制启用 MAC 随机化开关**（`force_randomization_switch`）——移入
- **覆写 AP MAC 地址开关**（`ap_mac_override_switch`）——移入
- 不添加其他设置项（用户确认）

### 关于页 About
- 应用图标 + 应用名 + 版本号（`BuildConfig.VERSION_NAME` 自动读取，不硬编码）
- **本项目地址**：https://github.com/Rillwyn/android-mac-editor —— 显示「项目名 + URL」（用户确认）
- **原项目地址**：https://github.com/jqssun/android-mac-editor —— 显示「项目名 + URL」（用户确认）
- 原项目链接下加**来源说明**：「Based on the original project's approach, fully rewritten with the libxposed Modern Xposed API (API 101)」/「基于原项目思路，使用 libxposed Modern Xposed API（API 101）全面重写」（用户确认）
- **当前维护者**：Rillwyn（用户确认）
- 链接整行可点击（`Intent.ACTION_VIEW` 打开浏览器）

## 五、代码改动清单

1. `gradle/libs.versions.toml`：新增 `viewpager2` 版本与库声明
2. `app/build.gradle.kts`：`dependencies` 增加 viewpager2；`versionCode = 11`、`versionName = "0.1.0"`
3. `MainActivity.kt`：重构为容器——ViewPager2 的 FragmentStateAdapter + BottomNavigationView 联动（点 Tab 切页、滑动同步选中）；移除 `onCreateOptionsMenu` 语言菜单；语言切换后恢复原 tab
4. 新增 `HomeFragment.kt`：状态卡更新（`_updateStatusCard` 逻辑迁入）、hook 开关、MAC 卡交互（输入/生成/应用/校验/广播）、`onResume` 广播接收器注册 + DataChannel 拉取系统 MAC、footer_note
5. 新增 `SettingsFragment.kt`：语言行内单选（保存 prefs + recreate）、强制随机化开关、AP 覆写开关监听
6. 新增 `AboutFragment.kt`：图标/名称/版本/链接（ACTION_VIEW）/来源说明/维护者
7. 布局资源：`activity_main.xml` 重构 + 三个 fragment 布局
8. 字符串资源：新增 Tab 标题、页面标题、关于页字段、来源说明等（中英双语）
9. 图标资源：BottomNavigationView 三个 Tab 的 vector 图标（主页/设置/关于），现有 drawable 补充

## 六、验证

1. `assembleDebug` 安装真机验证：三页面切换（点 Tab + 滑动）、MAC 应用生效、语言切换（含恢复原页面）、激活状态显示、广播/DataChannel 正常
2. `assembleRelease` 构建发布包（v0.1.0）

## 七、风险与注意

- **语言切换**：`recreate()` 后需记住当前 tab（prefs 或 SavedInstanceState），避免切回主页
- **ViewPager2 Fragment 生命周期**：非当前页 Fragment 进入 STARTED 状态，HomeFragment 的 `onResume/onPause` 随页面可见性触发，广播接收器注册/注销逻辑保持正确
- **MAC 输入框状态**：FragmentStateAdapter 保存 Fragment 状态；主页已有「从 prefs 恢复已保存 MAC」逻辑，切页不丢输入
- **Xposed 模块入口不受影响**：`App.kt` / `HookEntry.kt` / hookers 均不改动
