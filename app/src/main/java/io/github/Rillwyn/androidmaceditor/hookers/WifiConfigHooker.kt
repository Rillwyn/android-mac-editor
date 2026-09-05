package io.github.Rillwyn.androidmaceditor.hookers

import android.content.res.Resources
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.Rillwyn.androidmaceditor.TAG

/**
 * 系统资源 Hook 实现（system_server 作用域，现代 libxposed API）。
 *
 * Hook `Resources.getBoolean(int)`，每次调用时按资源名拦截，
 * 将以下系统 bool 资源强制返回 true（按用户开关决定），使系统认为
 * Wi-Fi / Wi-Fi Direct / 热点均支持 MAC 随机化：
 *
 * - config_wifi_connected_mac_randomization_supported
 * - config_wifi_p2p_mac_randomization_supported
 * - config_wifi_ap_mac_randomization_supported
 *
 * 注：现代 Xposed API 已移除 legacy 的“资源替换（XResources）”能力，
 * 这里与旧实现一致采用普通方法 Hook（拦截链模型），在 framework 侧直接替换返回值。
 * 开关值来自框架数据库的 Remote Preferences，并注册变更监听即时生效。
 */
object WifiConfigHooker {

    private val TARGET_KEYS = setOf(
        "config_wifi_connected_mac_randomization_supported",
        "config_wifi_p2p_mac_randomization_supported",
        "config_wifi_ap_mac_randomization_supported"
    )

    /** Remote Preferences group 名（与模块 App 侧、WifiServiceHooker 保持一致） */
    private const val PREFS_NAME = "io.github.Rillwyn.androidmaceditor"

    /** 缓存用户开关：避免在每次 getBoolean 调用时都走 binder 读远程偏好 */
    @Volatile
    private var forceShowMacRandomization = true

    /** 缓存模块总开关（与 WifiServiceHooker 联动的本地镜像） */
    @Volatile
    private var hookActive = true

    /**
     * 在 [module]（system_server 实例）中安装 Hook。
     *
     * @param module 当前模块实例（提供 hook/log/getRemotePreferences 能力）
     * @param loader system_server 类加载器
     */
    fun install(module: XposedModule, loader: ClassLoader) {
        // 读取远程偏好并监听变化（framework 数据库，跨进程实时同步）
        val prefs = runCatching { module.getRemotePreferences(PREFS_NAME) }.getOrNull()
        if (prefs != null) {
            hookActive = prefs.getBoolean("hookActive", true)
            forceShowMacRandomization = prefs.getBoolean("forceShowMacRandomization", true)
            prefs.registerOnSharedPreferenceChangeListener { _, _ ->
                hookActive = prefs.getBoolean("hookActive", hookActive)
                forceShowMacRandomization = prefs.getBoolean("forceShowMacRandomization", true)
                module.log(
                    Log.DEBUG, TAG,
                    "flags updated: hookActive=$hookActive, forceShowMacRandomization=$forceShowMacRandomization"
                )
            }
        }

        val resourcesClass = runCatching {
            Class.forName("android.content.res.Resources", false, loader)
        }.getOrNull()
        if (resourcesClass == null) {
            module.log(Log.WARN, TAG, "Resources class not found, skip")
            return
        }
        val method = runCatching {
            resourcesClass.getDeclaredMethod("getBoolean", Int::class.javaPrimitiveType)
        }.getOrNull() ?: run {
            module.log(Log.WARN, TAG, "Resources.getBoolean not found, skip")
            return
        }
        runCatching {
            module.hook(method).intercept { chain ->
                val result = chain.proceed()
                if (!hookActive) return@intercept result
                if (!forceShowMacRandomization) return@intercept result
                val res = chain.thisObject as? Resources ?: return@intercept result
                val id = chain.getArg(0) as? Int ?: return@intercept result
                val name = runCatching { res.getResourceEntryName(id) }.getOrNull()
                    ?: return@intercept result
                if (name in TARGET_KEYS) {
                    module.log(Log.DEBUG, TAG, "Forced $name to true")
                    return@intercept true
                }
                result
            }
        }.onFailure {
            module.log(Log.ERROR, TAG, "Hook Resources.getBoolean failed", it)
        }
    }
}
