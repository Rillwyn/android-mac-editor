package io.github.Rillwyn.androidmaceditor

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import io.github.Rillwyn.androidmaceditor.hookers.WifiConfigHooker
import io.github.Rillwyn.androidmaceditor.hookers.WifiServiceHooker

/** 全局日志标签 */
const val TAG = "MACEditor"

/**
 * 模块 Hook 入口 —— libxposed Modern Xposed API（API 101）。
 *
 * 与 legacy XposedBridge/YukiHookAPI 入口（@InjectYukiHookWithXposed + assets/xposed_init）不同：
 * - 入口类在 `META-INF/xposed/java_init.list` 中声明，无需注解处理器；
 * - 模块只需继承 [XposedModule]，框架会自动调用 `attachFramework`，构造器必须无参；
 * - 作用域在 `META-INF/xposed/scope.list`（本模块为 system_server），
 *   API 版本在 `META-INF/xposed/module.prop`（minApiVersion/targetApiVersion = 101）。
 */
class MacEditorModule : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "module loaded in process '${param.processName}' (systemServer=${param.isSystemServer})")
        log(
            Log.INFO, TAG,
            "framework: $frameworkName $frameworkVersion (versionCode=$frameworkVersionCode), API=$apiVersion"
        )
        val props = frameworkProperties
        log(
            Log.INFO, TAG,
            "capabilities: hookSystem=${props and XposedInterface.PROP_CAP_SYSTEM != 0L}, " +
                "remote=${props and XposedInterface.PROP_CAP_REMOTE != 0L}, " +
                "apiProtection=${props and XposedInterface.PROP_RT_API_PROTECTION != 0L}"
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(Log.INFO, TAG, "system server is starting, installing hooks...")
        val loader = param.classLoader
        // WifiNative / WifiVendorHal MAC 覆写 + 广播通道
        WifiServiceHooker.install(this, loader)
        // Resources.getBoolean 拦截：强制开启 Wi-Fi MAC 随机化支持位
        WifiConfigHooker.install(this, loader)
        log(Log.INFO, TAG, "all hooks installed")
    }
}
