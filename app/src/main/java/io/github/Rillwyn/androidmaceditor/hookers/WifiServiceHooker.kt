package io.github.Rillwyn.androidmaceditor.hookers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.MacAddress
import android.util.Log
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedModule
import io.github.Rillwyn.androidmaceditor.BuildConfig
import io.github.Rillwyn.androidmaceditor.MacBroadcastReceiver
import io.github.Rillwyn.androidmaceditor.TAG
import java.io.File
import java.lang.reflect.Method

/**
 * 系统框架（system_server）Hook 引擎 —— 现代 libxposed API（API 101）。
 *
 * 架构（吸收自 libxposed 重构 + Xposed-Modules-Repo PR #1 的多厂商/零点击特性）：
 * - 用户设置通过框架数据库的 **Remote Preferences** 跨进程读写（App 内读写，
 *   system_server 内只读 + 变更监听，替代旧 XSharedPreferences 方案）；
 * - 拦截链模型（`hook(Executable).intercept {}`）替代旧 `Member.hook` / finder 写法；
 * - **多厂商发现**：遍历 AOSP 与 Samsung / Xiaomi / MediaTek / Huawei 等定制
 *   `WifiNative` / `WifiVendorHal` 类；监听 `SystemServiceManager.loadClassFromLoader`
 *   与 `ServiceManager.addService("wifi")`，并在延迟兜底线程按需解析 Wi-Fi 服务 ClassLoader；
 * - **动态 AP 接口**：缓存 `ap*` / `softap*` / `swlan*` / `wlanN` 等接口名，
 *   覆写开关开启时同步应用到 AP 接口；
 * - **零点击即时生效**：App 内开关/MAC 变更即发送 [ACTION_CONFIG_CHANGED]，
 *   system_server 侧立即把自定义 MAC 应用到 STA 与（可选）AP 接口。
 */
object WifiServiceHooker {

    /** 应用点击“应用 MAC”/主动应用时发送的广播 Action */
    const val ACTION_APPLY_MAC = "${BuildConfig.APPLICATION_ID}.ACTION_APPLY_MAC"

    /** 应用设置变化（开关/MAC）时发送的广播 Action —— 触发零点击即时生效 */
    const val ACTION_CONFIG_CHANGED = "${BuildConfig.APPLICATION_ID}.ACTION_CONFIG_CHANGED"

    /** 应用打开/切回前台时查询当前系统 MAC 的广播 Action */
    const val ACTION_QUERY_MAC = "${BuildConfig.APPLICATION_ID}.ACTION_QUERY_MAC"

    /** 广播中携带的目标 MAC 键名 */
    const val EXTRA_MAC = "mac"

    /** 系统将当前 MAC 广播给应用（用于 UI 展示，尽力而为） */
    const val ACTION_MAC_DETECTED = "${BuildConfig.APPLICATION_ID}.ACTION_MAC_DETECTED"

    /** Remote Preferences group 名（与模块 App 侧保持一致） */
    const val PREFS_NAME = "io.github.Rillwyn.androidmaceditor"

    /** 目标 Wi-Fi Native 类列表（AOSP + 厂商定制 ROM） */
    private val TARGET_WIFI_NATIVE_CLASSES = listOf(
        "com.android.server.wifi.WifiNative",
        "com.android.server.wifi.SemWifiNative",
        "com.samsung.android.server.wifi.SemWifiNative",
        "com.android.server.wifi.MiuiWifiNative",
        "com.mediatek.server.wifi.MtkWifiNative",
        "com.android.server.wifi.HwWifiNative",
        "com.huawei.server.wifi.HwWifiNative"
    )

    /** 目标 Wi-Fi HAL 类列表（AOSP + Samsung 等厂商定制） */
    private val TARGET_WIFI_HAL_CLASSES = listOf(
        "com.android.server.wifi.WifiVendorHal",
        "com.android.server.wifi.SemWifiVendorHal",
        "com.samsung.android.server.wifi.SemWifiVendorHal"
    )

    /** 厂商出厂 MAC 存储候选文件（多厂商回退） */
    private val FACTORY_MAC_FILES = listOf(
        "/mnt/vendor/persist/qca6390/wlan_mac.bin",
        "/mnt/vendor/persist/wlan_mac.bin",
        "/persist/wlan_mac.bin",
        "/vendor/firmware/wlan/qca_cld/WCNSS_qcom_cfg.ini",
        "/efs/wifi/.mac.info",
        "/sec_efs/wifi/.mac.info",
        "/data/vendor/wifi/mac_addr",
        "/data/vendor/mac_addr"
    )

    /** 模块实例（system_server 内），提供 hook / log / 远程偏好能力 */
    @Volatile
    private var module: XposedModule? = null

    /** 缓存的 WifiNative 实例（构造时缓存，保证“应用 MAC”随时可用） */
    @Volatile
    private var nativeInstance: Any? = null

    /** 最近一次捕获的 STA 客户端接口名（如 wlan0） */
    @Volatile
    private var lastIface: String? = null

    /** 最近一次捕获的 AP 热点接口名（ap0 / softap0 / swlan0 / wlan1…） */
    @Volatile
    private var lastApIface: String? = null

    /** 缓存 setStaMacAddress 方法引用 */
    @Volatile
    private var setStaMethod: Method? = null

    /** 缓存 setApMacAddress 方法引用 */
    @Volatile
    private var setApMethod: Method? = null

    /** 广播接收器是否已注册 */
    @Volatile
    private var applyReceiverRegistered = false

    /** 系统 Context（惰性获取后缓存） */
    @Volatile
    private var systemContext: Context? = null

    /** 最近一次系统设置的原始 MAC（用于主动拉取回退） */
    @Volatile
    private var lastBroadcastMac: String? = null

    // ---- 用户设置缓存（Remote Preferences 的本地镜像，避免热路径频繁跨进程读取）----
    @Volatile
    private var hookActive = true

    @Volatile
    private var apMacOverride = false

    @Volatile
    private var customMac = ""

    // ------------------------------------------------------------------
    // 安装入口
    // ------------------------------------------------------------------

    /**
     * 安装全部 Hook。由模块入口在 system_server 的
     * [io.github.libxposed.api.XposedModuleInterface.onSystemServerStarting] 回调中调用。
     */
    fun install(instance: XposedModule, loader: ClassLoader) {
        module = instance

        // Remote Preferences（框架数据库）：只读 + 变更监听
        val prefs = runCatching { instance.getRemotePreferences(PREFS_NAME) }.getOrNull()
        if (prefs != null) {
            hookActive = prefs.getBoolean("hookActive", true)
            apMacOverride = prefs.getBoolean("apMacOverride", false)
            customMac = prefs.getString("customMac", "") ?: ""
            prefs.registerOnSharedPreferenceChangeListener { _, key ->
                hookActive = prefs.getBoolean("hookActive", hookActive)
                apMacOverride = prefs.getBoolean("apMacOverride", apMacOverride)
                if (key == "customMac") {
                    customMac = prefs.getString("customMac", "") ?: ""
                }
                instance.log(
                    Log.DEBUG, TAG,
                    "preference changed: key=$key, hookActive=$hookActive, apMacOverride=$apMacOverride, customMac=$customMac"
                )
            }
        } else {
            instance.log(Log.WARN, TAG, "remote preferences unavailable, hooks use defaults")
        }

        // 策略 1：直接 Hook（目标类此时已可加载则立即生效）
        tryHookWifiClasses(loader)
        // 策略 2：监听 SystemServiceManager 加载 WifiService 的时机再次安装
        hookSystemServiceManager(loader)
        // 策略 3：监听 ServiceManager.addService("wifi") 捕获定制 ROM 的独立 ClassLoader
        hookServiceManager(loader)

        // 注册广播接收器（AMS 未就绪时延迟重试）
        registerApplyReceiver()
        Thread {
            try {
                Thread.sleep(4000)
            } catch (_: InterruptedException) {
            }
            registerApplyReceiver()
            // 兜底：Wi-Fi 服务较慢启动时按需解析其 ClassLoader 再装一次
            if (nativeInstance == null) {
                resolveWifiClassLoader()?.let { tryHookWifiClasses(it) }
            }
        }.apply { isDaemon = true }.start()

        instance.log(Log.INFO, TAG, "WifiServiceHooker installed")
    }

    // ------------------------------------------------------------------
    // 类发现与 Hook 安装
    // ------------------------------------------------------------------

    /** 遍历多厂商目标类安装 Hook */
    private fun tryHookWifiClasses(loader: ClassLoader) {
        (TARGET_WIFI_NATIVE_CLASSES + TARGET_WIFI_HAL_CLASSES).forEach { name ->
            hookClass(name, loader)
        }
    }

    /**
     * 对单个目标类安装：缓存实例（构造器）、拦截 setSta/setApMacAddress、
     * 记录接口名。
     */
    private fun hookClass(clazzName: String, loader: ClassLoader) {
        val inst = module ?: return
        val clazz = runCatching { Class.forName(clazzName, false, loader) }.getOrNull()
        if (clazz == null) {
            inst.log(Log.DEBUG, TAG, "class not found: $clazzName")
            return
        }
        // 缓存实例：Hook 全部构造器
        clazz.declaredConstructors.forEach { ctor ->
            runCatching {
                inst.hook(ctor).intercept { chain ->
                    val result = chain.proceed()
                    val obj = chain.thisObject
                    if (obj != null) {
                        nativeInstance = obj
                        inst.log(Log.DEBUG, TAG, "instance cached from $clazzName")
                    }
                    result
                }
            }.onFailure { inst.log(Log.DEBUG, TAG, "ctor hook failed on $clazzName: $it") }
        }
        // 拦截 setStaMacAddress / setApMacAddress
        hookStaApMethod(clazz, "STA")
        hookStaApMethod(clazz, "AP")
        // 记录 STA 接口名
        clazz.declaredMethods.firstOrNull { it.name == "setupForClientMode" }?.let { m ->
            runCatching {
                inst.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    (chain.getArg(0) as? String)?.let { lastIface = it }
                    result
                }
            }.onFailure { inst.log(Log.DEBUG, TAG, "setupForClientMode hook failed on $clazzName: $it") }
        }
    }

    /**
     * 监听 SystemServiceManager.loadClassFromLoader，捕获 Android 12+ APEX
     * service-wifi.jar 动态加载时机。
     */
    private fun hookSystemServiceManager(loader: ClassLoader) {
        val inst = module ?: return
        val ssm = runCatching {
            Class.forName("com.android.server.SystemServiceManager", false, loader)
        }.getOrNull() ?: return
        val method = runCatching {
            ssm.getDeclaredMethod("loadClassFromLoader", String::class.java, ClassLoader::class.java)
        }.getOrNull() ?: return
        runCatching {
            inst.hook(method).intercept { chain ->
                val result = chain.proceed()
                if (chain.getArg(0) == "com.android.server.wifi.WifiService") {
                    val cl = chain.getArg(1) as? ClassLoader
                    if (cl != null) {
                        inst.log(Log.DEBUG, TAG, "WifiService loaded via SystemServiceManager, reinstalling hooks")
                        tryHookWifiClasses(cl)
                    }
                }
                result
            }
        }.onFailure { inst.log(Log.DEBUG, TAG, "SystemServiceManager hook failed: $it") }
    }

    /**
     * 监听 [android.os.ServiceManager.addService]，捕获 `wifi` 服务注册时的
     * 独立 ClassLoader（部分厂商 ROM 用独立 classpath 加载 Wi-Fi 栈）。
     */
    private fun hookServiceManager(loader: ClassLoader) {
        val inst = module ?: return
        val sm = runCatching { Class.forName("android.os.ServiceManager", false, loader) }.getOrNull() ?: return
        sm.declaredMethods.filter { it.name == "addService" && it.parameterTypes.size >= 2 }.forEach { method ->
            runCatching {
                inst.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val name = chain.getArg(0) as? String
                    if (name != null && name.contains("wifi", ignoreCase = true)) {
                        val binder = chain.getArg(1)
                        val cl = binder?.javaClass?.classLoader
                        if (cl != null) {
                            inst.log(Log.DEBUG, TAG, "ServiceManager.addService($name) captured cl=$cl")
                            tryHookWifiClasses(cl)
                        }
                    }
                    result
                }
            }.onFailure { inst.log(Log.DEBUG, TAG, "ServiceManager hook failed: $it") }
        }
    }

    /** 通过反射查询 `wifi` Binder 服务以获取其 ClassLoader */
    private fun resolveWifiClassLoader(): ClassLoader? {
        return runCatching {
            val sm = Class.forName("android.os.ServiceManager")
            val binder = sm.getMethod("getService", String::class.java).invoke(null, "wifi")
            binder?.javaClass?.classLoader
        }.getOrNull()
    }

    /** Hook 指定类上的 setSta/setApMacAddress（(String, MacAddress) 签名） */
    private fun hookStaApMethod(clazz: Class<*>, type: String) {
        val inst = module ?: return
        val methodName = if (type == "STA") "setStaMacAddress" else "setApMacAddress"
        val method = runCatching {
            clazz.getDeclaredMethod(methodName, String::class.java, MacAddress::class.java)
        }.getOrNull() ?: return
        runCatching {
            inst.hook(method).intercept { chain -> macIntercept(chain, type) }
        }.onFailure { inst.log(Log.DEBUG, TAG, "hook $methodName on ${clazz.simpleName} failed: $it") }
    }

    /**
     * 拦截 MAC 设置调用：根据偏好替换为自定义 MAC，并记录 STA / AP 接口名。
     */
    private fun macIntercept(chain: Chain, type: String): Any? {
        val iface = chain.getArg(0) as? String
        module?.log(Log.DEBUG, TAG, "set${type}MacAddress called${iface?.let { " on $it" } ?: ""}")
        if (!hookActive) {
            module?.log(Log.DEBUG, TAG, "hookActive is off, skip")
            return chain.proceed()
        }
        if (iface == null) return chain.proceed()
        if (type == "STA") {
            lastIface = iface
        } else {
            lastApIface = iface
        }
        chain.thisObject?.let { nativeInstance = it }
        registerApplyReceiver()

        // 广播系统当前 MAC 给应用（用于 UI 展示，尽力而为）
        (chain.getArg(1) as? MacAddress)?.let { broadcastMac(it.toString()) }

        // AP 覆写开关：AP 接口只有在用户开启 AP 覆写时才替换
        if (type == "AP" && !apMacOverride) {
            return chain.proceed()
        }
        // STA 副接口保护：非主客户端接口（wlan0）默认不替换
        if (type == "STA" && iface.startsWith("wlan") && iface != "wlan0") {
            return chain.proceed()
        }
        val custom = customMac
        if (custom.isNotEmpty()) {
            return try {
                val mac = MacAddress.fromString(custom)
                module?.log(Log.DEBUG, TAG, "Replaced MAC with $custom on $iface ($type)")
                chain.proceed(arrayOf<Any>(iface, mac))
            } catch (t: Throwable) {
                module?.log(Log.ERROR, TAG, "Failed to parse custom MAC: $t")
                chain.proceed()
            }
        }
        module?.log(Log.DEBUG, TAG, "customMac is empty, no replacement")
        return chain.proceed()
    }

    // ------------------------------------------------------------------
    // 广播通道（模块 App <-> system_server）
    // ------------------------------------------------------------------

    /**
     * 注册接收 [ACTION_APPLY_MAC] / [ACTION_CONFIG_CHANGED] / [ACTION_QUERY_MAC]。
     */
    private fun registerApplyReceiver() {
        if (applyReceiverRegistered) return
        val ctx = getSystemContext() ?: return
        runCatching {
            ctx.registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            // 应用 MAC（广播直接携带目标 MAC，不依赖跨进程偏好读取时序）
                            ACTION_APPLY_MAC -> {
                                val mac = intent.getStringExtra(EXTRA_MAC)
                                if (mac.isNullOrEmpty()) applyMacDirectly(null) else applyMacDirectly(mac)
                            }
                            // 设置变化（零点击）：立即把当前配置应用到 STA / AP
                            ACTION_CONFIG_CHANGED -> {
                                applyMacDirectly(null)
                                if (apMacOverride) applyApMacDirectly()
                            }
                            // 查询 MAC：回发当前系统 MAC
                            ACTION_QUERY_MAC -> {
                                val mac = currentSystemMac()
                                if (mac.isNotEmpty()) {
                                    module?.log(Log.DEBUG, TAG, "Query: reply system MAC $mac")
                                    broadcastMac(mac)
                                }
                            }
                        }
                    }
                },
                IntentFilter().apply {
                    addAction(ACTION_APPLY_MAC)
                    addAction(ACTION_CONFIG_CHANGED)
                    addAction(ACTION_QUERY_MAC)
                },
                Context.RECEIVER_EXPORTED
            )
            applyReceiverRegistered = true
            module?.log(Log.DEBUG, TAG, "MAC receiver registered")
        }.onFailure {
            module?.log(Log.DEBUG, TAG, "MAC receiver not ready yet, will retry later")
        }
    }

    /** 直接应用 STA 自定义 MAC（利用缓存的 WifiNative 实例与接口名）。 */
    private fun applyMacDirectly(intentMac: String?) {
        val mac = intentMac ?: customMac
        if (mac.isEmpty() || !hookActive) return
        val native = nativeInstance
        val iface = lastIface
        if (native == null || iface == null) {
            module?.log(Log.WARN, TAG, "WifiNative not cached yet, will retry")
            retryApply(intentMac, applyAp = false)
            return
        }
        runCatching {
            val method = setStaMethod ?: native.javaClass
                .getDeclaredMethod("setStaMacAddress", String::class.java, MacAddress::class.java)
                .also { setStaMethod = it }
            method.invoke(native, iface, MacAddress.fromString(mac))
            module?.log(Log.DEBUG, TAG, "Directly applied STA MAC $mac on $iface")
        }.onFailure { module?.log(Log.ERROR, TAG, "Direct STA apply failed: $it") }
    }

    /** 动态探测 AP 热点接口并应用自定义 MAC（仅当 AP 覆写开启）。 */
    private fun applyApMacDirectly() {
        if (!apMacOverride || !hookActive) return
        val mac = customMac
        if (mac.isEmpty()) return
        val native = nativeInstance
        if (native == null) {
            module?.log(Log.WARN, TAG, "WifiNative not cached yet, will retry AP apply")
            retryApply(null, applyAp = true)
            return
        }
        // 优先使用最近捕获的 AP 接口，否则扫描活动网络接口
        val iface = lastApIface ?: discoverApIface()
        if (iface == null) {
            module?.log(Log.DEBUG, TAG, "No AP interface found, skip direct AP apply")
            return
        }
        runCatching {
            val method = setApMethod ?: native.javaClass
                .getDeclaredMethod("setApMacAddress", String::class.java, MacAddress::class.java)
                .also { setApMethod = it }
            method.invoke(native, iface, MacAddress.fromString(mac))
            module?.log(Log.DEBUG, TAG, "Directly applied AP MAC $mac on $iface")
        }.onFailure { module?.log(Log.ERROR, TAG, "Direct AP apply failed: $it") }
    }

    /** 扫描系统网络接口，识别 ap、softap、swlan、wlanN 等热点命名。 */
    private fun discoverApIface(): String? {
        return runCatching {
            val names = (File("/sys/class/net").listFiles() ?: emptyArray()).mapNotNull { it.name }
            names.firstOrNull { it.startsWith("softap") }
                ?: names.firstOrNull { it.startsWith("ap") }
                ?: names.firstOrNull { it.startsWith("swlan") }
                ?: names.firstOrNull { Regex("^wlan[1-9]").matches(it) }
        }.getOrNull()
    }

    /** 实例/接口未就绪时延迟重试（约 8 秒内）。 */
    private fun retryApply(intentMac: String?, applyAp: Boolean) {
        Thread {
            repeat(8) {
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                if (nativeInstance != null) {
                    if (applyAp) applyApMacDirectly() else applyMacDirectly(intentMac)
                    return@Thread
                }
            }
            module?.log(Log.WARN, TAG, "Give up applying MAC: WifiNative never became available")
        }.apply { isDaemon = true }.start()
    }

    /** 将系统当前 MAC 广播给模块应用（显式组件，写回本地缓存用于状态卡展示）。 */
    private fun broadcastMac(mac: String) {
        val ctx = getSystemContext() ?: return
        runCatching {
            val intent = Intent(ACTION_MAC_DETECTED).apply {
                putExtra(MacBroadcastReceiver.EXTRA_MAC, mac)
                setClassName(BuildConfig.APPLICATION_ID, MacBroadcastReceiver::class.java.name)
            }
            ctx.sendBroadcast(intent)
            lastBroadcastMac = mac
            module?.log(Log.DEBUG, TAG, "Broadcasted system MAC $mac")
        }.onFailure { module?.log(Log.ERROR, TAG, "Broadcast MAC failed: $it") }
    }

    // ------------------------------------------------------------------
    // 系统 MAC 读取
    // ------------------------------------------------------------------

    /**
     * 获取系统原始 MAC（出厂 MAC 优先，多厂商多级回退）：
     * 1. 反射 WifiNative.getFactoryMacAddress / getStaFactoryMacAddress（Android 12+ / 厂商扩展）；
     * 2. 读取厂商出厂 MAC 文件（高通 wlan_mac.bin、三星 EFS、/data/vendor 等）；
     * 3. 最近一次系统设置的原始 MAC；
     * 4. getStaMacAddress / /sys/class/net/wlan0/address（仅回退）。
     */
    private fun currentSystemMac(): String {
        val native = nativeInstance
        val iface = lastIface ?: "wlan0"
        if (native != null) {
            val factoryCandidates = listOf(
                "getStaFactoryMacAddress" to arrayOf<Class<*>>(String::class.java),
                "getStaFactoryMacAddress" to emptyArray<Class<*>>(),
                "getFactoryMacAddress" to arrayOf<Class<*>>(String::class.java),
                "getFactoryMacAddress" to emptyArray<Class<*>>()
            )
            for ((methodName, params) in factoryCandidates) {
                runCatching {
                    val m = native.javaClass.getDeclaredMethod(methodName, *params)
                    val args = if (params.isEmpty()) emptyArray<Any?>() else arrayOf<Any?>(iface)
                    (m.invoke(native, *args) as? MacAddress)?.toString()
                }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                    module?.log(Log.DEBUG, TAG, "Factory MAC via $methodName: $it")
                    return it
                }
            }
        } else {
            module?.log(Log.DEBUG, TAG, "nativeInstance is null, skip factory MAC reflection")
        }
        // 厂商出厂 MAC 文件（通用解析）
        for (path in FACTORY_MAC_FILES) {
            val mac = readMacFromFile(path) ?: continue
            module?.log(Log.DEBUG, TAG, "Factory MAC via $path: $mac")
            return mac
        }
        // 最近一次系统设置的原始 MAC
        lastBroadcastMac?.takeIf { it.isNotEmpty() }?.let { return it }
        // 回退：当前生效 MAC
        if (native != null) {
            runCatching {
                val m = native.javaClass.getDeclaredMethod("getStaMacAddress", String::class.java)
                (m.invoke(native, iface) as? MacAddress)?.toString()
            }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        runCatching {
            val addr = File("/sys/class/net/wlan0/address").readText().trim().uppercase()
            if (addr.matches(Regex("^[0-9A-F:]{17}$"))) return addr
        }.getOrNull()
        return ""
    }

    /** 从厂商文件内容中提取并格式化 MAC 地址。 */
    private fun readMacFromFile(path: String): String? {
        return runCatching {
            val f = File(path)
            if (!f.exists() || !f.canRead()) return null
            val text = f.readText()
            parseMacText(text)
        }.getOrNull()
    }

    private fun parseMacText(text: String): String? {
        // 优先匹配 xx:xx:xx:xx:xx:xx / xx-xx-xx-xx-xx-xx，再回退 12 位十六进制
        val colon = Regex("(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}").find(text)
        val raw = colon?.value ?: Regex("[0-9A-Fa-f]{12}").find(text)?.value ?: return null
        val hex = raw.replace(":", "").replace("-", "")
        if (hex.length != 12) return null
        val mac = hex.chunked(2).joinToString(":").uppercase()
        if (mac == "00:00:00:00:00:00") return null
        return mac
    }

    /** 获取系统框架 Context（惰性获取并缓存）。 */
    private fun getSystemContext(): Context? {
        systemContext?.let { return it }
        val ctx = runCatching {
            val at = Class.forName("android.app.ActivityThread")
            at.getMethod("currentApplication").invoke(null) as? Context
                ?: runCatching {
                    val activityThread = at.getMethod("currentActivityThread").invoke(null)
                    activityThread?.javaClass?.getMethod("getSystemContext")
                        ?.invoke(activityThread) as? Context
                }.getOrNull()
        }.getOrNull()
        systemContext = ctx
        return ctx
    }
}
