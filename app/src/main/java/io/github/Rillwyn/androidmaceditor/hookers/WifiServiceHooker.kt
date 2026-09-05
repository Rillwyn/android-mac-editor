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
 * 系统框架（system_server）Hook 实现 —— 现代 libxposed API（API 101）。
 *
 * - 用户设置通过框架数据库的 **Remote Preferences** 跨进程读写：
 *   模块 App 内经 [io.github.libxposed.service.XposedService] 可读可写，
 *   这里（system_server）经 [XposedModule.getRemotePreferences] 只读，并注册
 *   变更监听，开关与自定义 MAC 修改后无需重启即生效（替代旧 XSharedPreferences 方案）；
 * - 使用拦截链模型（`hook(Executable).intercept {}`）替代旧 `Member.hook` / finder 写法；
 * - Hook `WifiNative` 全部构造器缓存实例，保证“应用 MAC”随时可用。
 */
object WifiServiceHooker {

    /** 应用点击“应用 MAC”时发送的广播 Action */
    const val ACTION_APPLY_MAC = "${BuildConfig.APPLICATION_ID}.ACTION_APPLY_MAC"

    /** 应用打开/切回前台时查询当前系统 MAC 的广播 Action（替代旧 YukiHookDataChannel） */
    const val ACTION_QUERY_MAC = "${BuildConfig.APPLICATION_ID}.ACTION_QUERY_MAC"

    /** 广播中携带的目标 MAC 键名 */
    const val EXTRA_MAC = "mac"

    /** 系统将当前 MAC 广播给应用（用于 UI 展示，尽力而为） */
    const val ACTION_MAC_DETECTED = "${BuildConfig.APPLICATION_ID}.ACTION_MAC_DETECTED"

    /** Remote Preferences group 名（与模块 App 侧保持一致） */
    const val PREFS_NAME = "io.github.Rillwyn.androidmaceditor"

    private const val WIFI_NATIVE_CLASS = "com.android.server.wifi.WifiNative"
    private const val WIFI_VENDOR_HAL_CLASS = "com.android.server.wifi.WifiVendorHal"

    /** 模块实例（system_server 内），提供 hook / log / 远程偏好能力 */
    @Volatile
    private var module: XposedModule? = null

    /** 缓存的 WifiNative 实例（构造时缓存，保证“应用 MAC”随时可用） */
    @Volatile
    private var nativeInstance: Any? = null

    /** 当前 STA 接口名（wlan0） */
    @Volatile
    private var lastIface: String? = null

    /** 缓存 setStaMacAddress 方法引用 */
    @Volatile
    private var setStaMethod: Method? = null

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

    /**
     * 安装全部 Hook。由 [io.github.Rillwyn.androidmaceditor.MacEditorModule] 在
     * [io.github.libxposed.api.XposedModuleInterface.onSystemServerStarting] 回调中调用。
     *
     * @param instance 模块实例
     * @param loader   system_server 类加载器
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
                instance.log(Log.DEBUG, TAG, "preference changed: key=$key, hookActive=$hookActive, apMacOverride=$apMacOverride, customMac=$customMac")
            }
        } else {
            instance.log(Log.WARN, TAG, "remote preferences unavailable, hooks use defaults")
        }

        // 尝试 1：直接 Hook（WifiNative 等已可加载时立即生效）
        tryHookWifiNative(loader)

        // 尝试 2：监听 SystemServiceManager 加载 WifiService 的时刻再次 Hook，
        // 确保任何加载时序下都能装上
        hookSystemServiceManager(loader)

        // 注册“应用 MAC”/“查询 MAC”广播接收器；系统早期 AMS 未就绪会失败，
        // 稍后由延迟任务重试。
        registerApplyReceiver()
        Thread {
            try {
                Thread.sleep(5000)
            } catch (_: InterruptedException) {
            }
            registerApplyReceiver()
        }.apply { isDaemon = true }.start()

        instance.log(Log.INFO, TAG, "WifiServiceHooker installed")
    }

    // ------------------------------------------------------------------
    // 类查找与 Hook 安装
    // ------------------------------------------------------------------

    /**
     * 监听 SystemServiceManager.loadClassFromLoader，当 WifiService 类被加载时
     * 使用其 ClassLoader 重新 Hook WifiNative（保证类已就绪）。
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
                        inst.log(Log.DEBUG, TAG, "WifiService class loaded, (re)installing WifiNative hooks")
                        tryHookWifiNative(cl)
                    }
                }
                result
            }
        }.onFailure {
            inst.log(Log.DEBUG, TAG, "SystemServiceManager hook failed: $it")
        }
    }

    /**
     * Hook WifiNative：缓存实例（构造器）、拦截 setStaMacAddress/setApMacAddress、
     * 记录 STA 接口名。
     */
    private fun tryHookWifiNative(loader: ClassLoader) {
        val inst = module ?: return
        val nativeClass = runCatching {
            Class.forName(WIFI_NATIVE_CLASS, false, loader)
        }.getOrNull()
        if (nativeClass == null) {
            inst.log(Log.WARN, TAG, "WifiNative class not found (will retry on WifiService load)")
            return
        }
        // 诊断：打印可取 MAC 的系统方法
        runCatching {
            val vendorHal = runCatching { Class.forName(WIFI_VENDOR_HAL_CLASS, false, loader) }.getOrNull()
            val macMethods = (nativeClass.declaredMethods + (vendorHal?.declaredMethods.orEmpty()))
                .filter { it.name.contains("Mac", ignoreCase = true) }
                .map { "${it.declaringClass.simpleName}.${it.name}(${it.parameterTypes.joinToString(",") { p -> p.simpleName }})" }
                .distinct()
                .sorted()
            inst.log(Log.DEBUG, TAG, "Mac-related methods: $macMethods")
        }.onFailure { inst.log(Log.DEBUG, TAG, "Method list dump failed: $it") }

        // 缓存 WifiNative 实例：Hook 全部构造器，系统一创建实例即缓存
        nativeClass.declaredConstructors.forEach { ctor ->
            runCatching {
                inst.hook(ctor).intercept { chain ->
                    val result = chain.proceed()
                    val obj = chain.thisObject
                    if (obj != null) {
                        nativeInstance = obj
                        inst.log(Log.DEBUG, TAG, "WifiNative instance cached")
                    }
                    result
                }
            }.onFailure { inst.log(Log.DEBUG, TAG, "ctor hook failed: $it") }
        }
        // 拦截 setStaMacAddress / setApMacAddress（WifiNative 与 WifiVendorHal）
        hookStaApMethods(WIFI_NATIVE_CLASS, loader, "STA")
        hookStaApMethods(WIFI_NATIVE_CLASS, loader, "AP")
        hookStaApMethods(WIFI_VENDOR_HAL_CLASS, loader, "STA")
        hookStaApMethods(WIFI_VENDOR_HAL_CLASS, loader, "AP")
        // 记录 STA 接口名
        nativeClass.declaredMethods.firstOrNull { it.name == "setupForClientMode" }?.let { m ->
            runCatching {
                inst.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    (chain.getArg(0) as? String)?.let { lastIface = it }
                    result
                }
            }.onFailure { inst.log(Log.DEBUG, TAG, "setupForClientMode hook failed: $it") }
        }
        inst.log(Log.INFO, TAG, "WifiNative hooks installed")
    }

    /**
     * Hook 指定类上的 setStaMacAddress / setApMacAddress（(String, MacAddress) 签名）。
     */
    private fun hookStaApMethods(clazzName: String, loader: ClassLoader, type: String) {
        val inst = module ?: return
        val clazz = runCatching { Class.forName(clazzName, false, loader) }.getOrNull() ?: return
        val methodName = if (type == "STA") "setStaMacAddress" else "setApMacAddress"
        val method = runCatching {
            clazz.getDeclaredMethod(methodName, String::class.java, MacAddress::class.java)
        }.getOrNull() ?: return
        runCatching {
            inst.hook(method).intercept { chain -> macIntercept(chain, type) }
        }.onFailure {
            inst.log(Log.DEBUG, TAG, "hook $methodName failed: $it")
        }
    }

    /**
     * 拦截 MAC 设置调用：根据偏好替换为自定义 MAC。
     * 拦截链模型：修改参数后以 `chain.proceed(新参数)` 继续执行原方法。
     */
    private fun macIntercept(chain: Chain, type: String): Any? {
        val iface = chain.getArg(0) as? String
        module?.log(Log.DEBUG, TAG, "set${type}MacAddress called${iface?.let { " on $it" } ?: ""}")
        if (!hookActive) {
            module?.log(Log.DEBUG, TAG, "hookActive is off, skip")
            return chain.proceed()
        }
        if (iface == null) return chain.proceed()
        lastIface = iface
        chain.thisObject?.let { nativeInstance = it }

        // 系统就绪后（首次 MAC 调用时）确保接收器已注册
        registerApplyReceiver()

        // 广播系统当前 MAC 给应用（用于 UI 展示，尽力而为）
        (chain.getArg(1) as? MacAddress)?.let { broadcastMac(it.toString()) }

        // AP 覆写开关：非 wlan0 的 AP 接口默认不替换，避免热点无法启动
        if (iface.startsWith("wlan") && iface != "wlan0" && !apMacOverride) {
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
     * 注册接收 [ACTION_APPLY_MAC] / [ACTION_QUERY_MAC] 广播的接收器。
     * system 未就绪时静默失败，稍后重试。
     */
    private fun registerApplyReceiver() {
        if (applyReceiverRegistered) return
        val ctx = getSystemContext() ?: return
        runCatching {
            ctx.registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            // 应用 MAC：广播可直接携带目标 MAC（不依赖跨进程偏好读取时序）
                            ACTION_APPLY_MAC -> {
                                val mac = intent.getStringExtra(EXTRA_MAC)
                                if (mac.isNullOrEmpty()) applyMacDirectly(null)
                                else applyMacDirectly(mac)
                            }
                            // 查询 MAC：回复 ACTION_MAC_DETECTED 携带当前系统 MAC
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
                    addAction(ACTION_QUERY_MAC)
                },
                Context.RECEIVER_EXPORTED
            )
            applyReceiverRegistered = true
            module?.log(Log.DEBUG, TAG, "Apply/Query MAC receiver registered")
        }.onFailure {
            // system_server 启动早期 AMS 未就绪时首次注册会失败，
            // 属正常现象（延迟任务会重试），仅降级为 debug 日志避免误报。
            module?.log(Log.DEBUG, TAG, "Apply receiver not ready yet, will retry later")
        }
    }

    /**
     * 直接应用 MAC（利用缓存的 WifiNative 实例与接口名）。
     *
     * @param intentMac 广播携带的目标 MAC；为空时回退读取偏好缓存。
     *                  WifiNative 实例尚未缓存（重启后 WiFi 未初始化）时自动延迟重试。
     */
    private fun applyMacDirectly(intentMac: String?) {
        val mac = intentMac ?: customMac
        if (mac.isEmpty()) return
        val native = nativeInstance
        val iface = lastIface
        if (native == null || iface == null) {
            module?.log(Log.WARN, TAG, "WifiNative not cached yet, will retry")
            retryApplyMac(intentMac)
            return
        }
        runCatching {
            val method = setStaMethod ?: native.javaClass
                .getDeclaredMethod("setStaMacAddress", String::class.java, MacAddress::class.java)
                .also { setStaMethod = it }
            method.invoke(native, iface, MacAddress.fromString(mac))
            module?.log(Log.DEBUG, TAG, "Directly applied MAC $mac on $iface")
        }.onFailure {
            module?.log(Log.ERROR, TAG, "Direct apply failed: $it")
        }
    }

    /**
     * WifiNative 实例未就绪时延迟重试（最多约 8 秒），直到实例可用。
     */
    private fun retryApplyMac(intentMac: String?) {
        Thread {
            repeat(8) {
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                if (nativeInstance != null && lastIface != null) {
                    applyMacDirectly(intentMac)
                    return@Thread
                }
            }
            module?.log(Log.WARN, TAG, "Give up applying MAC: WifiNative never became available")
        }.apply { isDaemon = true }.start()
    }

    /**
     * 将系统当前 MAC 广播给模块应用（显式组件，写回本地缓存用于状态卡展示）。
     */
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
        }.onFailure {
            module?.log(Log.ERROR, TAG, "Broadcast MAC failed: $it")
        }
    }

    // ------------------------------------------------------------------
    // 系统 MAC 读取
    // ------------------------------------------------------------------

    /**
     * 获取系统原始 MAC（出厂 MAC 优先，优先级从高到低）：
     * 1. 反射 WifiNative.getFactoryMacAddress(iface)（Android 12+，返回硬件出厂 MAC）；
     * 2. 解析高通 wlan_mac.bin（Intf0MacAddress）；
     * 3. 最近一次系统设置的原始 MAC（替换前捕获）；
     * 4. getStaMacAddress / /sys/class/net/wlan0/address（仅回退）。
     */
    private fun currentSystemMac(): String {
        val native = nativeInstance
        val iface = lastIface ?: "wlan0"
        if (native != null) {
            // 1. 出厂 MAC：尝试多种方法名与签名
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
            module?.log(Log.DEBUG, TAG, "nativeInstance is null, skip getFactoryMacAddress")
        }
        // 2. 高通 wlan_mac.bin（Intf0MacAddress=xxxxxxxxxxxx）
        runCatching {
            val f = File("/mnt/vendor/persist/qca6390/wlan_mac.bin")
            module?.log(Log.DEBUG, TAG, "wlan_mac.bin exists=${f.exists()} readable=${f.canRead()}")
            if (f.exists()) {
                val text = f.readText()
                val m = Regex("Intf0MacAddress=([0-9A-Fa-f]{12})").find(text)
                m?.groupValues?.get(1)?.chunked(2)?.joinToString(":")?.uppercase()?.let {
                    module?.log(Log.DEBUG, TAG, "Factory MAC via wlan_mac.bin: $it")
                    return it
                }
            }
        }.onFailure { module?.log(Log.DEBUG, TAG, "wlan_mac.bin read failed: $it") }
        // 3. 最近一次系统设置的原始 MAC（替换前捕获）
        lastBroadcastMac?.takeIf { it.isNotEmpty() }?.let { return it }
        // 4. 回退：当前生效 MAC
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

    /**
     * 获取系统框架 Context（惰性获取并缓存）。
     * system_server 中 `ActivityThread.currentApplication()` 可能为 null，
     * 因此回退到 `currentActivityThread().getSystemContext()`。
     */
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
