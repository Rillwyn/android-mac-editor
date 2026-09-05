package io.github.Rillwyn.androidmaceditor

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 模块应用入口（现代 libxposed API / API 101）。
 *
 * 与 legacy（YukiHookAPI 的 ModuleApplication 自 Hook）不同：
 * 现代框架**不再向模块自身进程注入 Hook**，应用与框架的通信改为
 * [XposedService]：模块 App 进程启动后，框架会把一个服务 Binder 推送过来
 * （见 [XposedServiceHelper]）。据此可以：
 * - 判断模块是否激活（收到 Service = 模块已在作用域中生效）；
 * - 通过 Remote Preferences 把用户设置写入框架数据库，供 system_server 内 Hook 读取；
 * - 查询/请求作用域等。
 */
class App : Application(), XposedServiceHelper.OnServiceListener {

    /** 模块激活状态回调接口（UI 据此刷新状态卡） */
    interface ServiceStateListener {
        fun onServiceStateChanged(active: Boolean)
    }

    companion object {
        /** Remote Preferences group 名（与 system_server 侧 Hooker 的 PREFS_NAME 一致） */
        const val REMOTE_GROUP = "io.github.Rillwyn.androidmaceditor"

        @Volatile
        private var mService: XposedService? = null

        private val serviceStateListeners = CopyOnWriteArraySet<ServiceStateListener>()
        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

        /** 当前是否已绑定到 Xposed 框架（模块是否激活） */
        fun isModuleActive(): Boolean = mService != null

        /** 当前 XposedService（未激活时为 null） */
        fun currentService(): XposedService? = mService

        /**
         * 当前可用（已绑定框架）的 Remote Preferences；未激活、框架不支持远程偏好
         * （缺少 PROP_CAP_REMOTE）或服务瞬断时返回 null（UI 自动回退本地缓存）。
         */
        fun remotePrefs(): SharedPreferences? {
            val service = mService ?: return null
            return runCatching { service.getRemotePreferences(REMOTE_GROUP) }.getOrNull()
        }

        /** 模块本地偏好（作为未激活时的缓存与激活后同步的来源） */
        fun localPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences(REMOTE_GROUP, Context.MODE_PRIVATE)

        fun addServiceStateListener(listener: ServiceStateListener, notifyImmediately: Boolean) {
            serviceStateListeners.add(listener)
            if (notifyImmediately) {
                mainHandler.post { listener.onServiceStateChanged(mService != null) }
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            serviceStateListeners.remove(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        mService = service
        // 每次连接成功：把本地缓存设置同步到框架 Remote Preferences，
        // 保证“先设置、后激活”的历史数据也能立即被 system_server 侧 Hook 读到。
        runCatching { syncLocalToRemote() }
        dispatchServiceState()
    }

    override fun onServiceDied(service: XposedService) {
        if (mService === service) mService = null
        dispatchServiceState()
    }

    private fun dispatchServiceState() {
        val active = mService != null
        mainHandler.post {
            for (listener in serviceStateListeners) {
                listener.onServiceStateChanged(active)
            }
        }
    }

    /**
     * 本地缓存 → 远程偏好 单向同步（键级：仅推送本地已存在的键）。
     */
    private fun syncLocalToRemote() {
        val service = mService ?: return
        val local = localPrefs(this)
        val remote = service.getRemotePreferences(REMOTE_GROUP)
        val keys = listOf("hookActive", "customMac", "apMacOverride", "forceShowMacRandomization")
        val editor = remote.edit() ?: return
        var dirty = false
        for (key in keys) {
            if (local.contains(key)) {
                when (key) {
                    "customMac" -> editor.putString(key, local.getString(key, ""))
                    else -> editor.putBoolean(key, local.getBoolean(key, false))
                }
                dirty = true
            }
        }
        if (dirty) editor.apply()
    }
}
