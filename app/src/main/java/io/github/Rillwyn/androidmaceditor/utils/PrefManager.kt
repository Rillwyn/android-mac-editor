package io.github.Rillwyn.androidmaceditor.utils

import android.content.Context
import android.content.SharedPreferences
import io.github.Rillwyn.androidmaceditor.App

/**
 * 模块偏好设置管理（现代 libxposed API / API 101）。
 *
 * 统一走 **Remote Preferences**（框架数据库）：
 * - 模块 App 内经 [App.remotePrefs] 可读可写（未激活时为 null）；
 * - system_server（宿主进程）内由 Hook 侧只读同一份数据，修改即时生效；
 * - 本地 SharedPreferences 仅作为未激活时的缓存与“先设置后激活”的同步来源。
 *
 * 读写策略：读 = 远程优先（激活后与 Hook 侧看到的完全一致），
 * 未激活回退本地；写 = 本地与远程（若可用）同时写入。
 */
object PrefManager {

    /** 偏好/Remote group 名（与 system_server 侧保持一致，兼容历史数据） */
    const val PREFS_NAME = "io.github.Rillwyn.androidmaceditor"

    private fun local(context: Context): SharedPreferences = App.localPrefs(context)

    private fun remote(): SharedPreferences? = App.remotePrefs()

    private fun readBool(context: Context, key: String, def: Boolean): Boolean =
        remote()?.getBoolean(key, def) ?: local(context).getBoolean(key, def)

    private fun readString(context: Context, key: String, def: String): String =
        remote()?.getString(key, def) ?: local(context).getString(key, def) ?: def

    private fun writeBool(context: Context, key: String, value: Boolean) {
        local(context).edit().putBoolean(key, value).apply()
        remote()?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun writeString(context: Context, key: String, value: String) {
        local(context).edit().putString(key, value).apply()
        remote()?.edit()?.putString(key, value)?.apply()
    }

    fun isHookOn(context: Context): Boolean = readBool(context, "hookActive", true)

    fun setHookState(context: Context, on: Boolean) {
        writeBool(context, "hookActive", on)
    }

    fun getCustomMac(context: Context): String = readString(context, "customMac", "")

    fun setCustomMac(context: Context, mac: String) {
        writeString(context, "customMac", mac)
    }

    fun isForceShowMacRandomization(context: Context): Boolean =
        readBool(context, "forceShowMacRandomization", true)

    fun setForceShowMacRandomization(context: Context, on: Boolean) {
        writeBool(context, "forceShowMacRandomization", on)
    }

    fun isApMacOverride(context: Context): Boolean = readBool(context, "apMacOverride", false)

    fun setApMacOverride(context: Context, on: Boolean) {
        writeBool(context, "apMacOverride", on)
    }
}
