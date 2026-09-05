package io.github.Rillwyn.androidmaceditor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.Rillwyn.androidmaceditor.databinding.FragmentHomeBinding
import io.github.Rillwyn.androidmaceditor.hookers.WifiServiceHooker
import io.github.Rillwyn.androidmaceditor.utils.MacTextWatcher
import io.github.Rillwyn.androidmaceditor.utils.MacUtils
import io.github.Rillwyn.androidmaceditor.utils.PrefManager
import io.github.libxposed.service.XposedService

class HomeFragment : Fragment(), App.ServiceStateListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var updatingUI = false

    // 广播接收器，用于接收系统 MAC 更新（仅用于 UI 展示，不作为激活状态依据）
    private val macReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            _refreshDeviceMac()
            _updateStatusCard()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _setupToggles()
        _setupMacCard()
        binding.footerNote.text = getString(
            R.string.footer_note,
            getString(R.string.force_mac_randomization_label)
        )
        // 偏好立即可用，无需异步绑定
        _refreshAll()
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            requireContext(),
            macReceiver,
            IntentFilter(MacBroadcastReceiver.ACTION_MAC_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // 监听 XposedService 绑定状态（模块激活/失活实时反映到状态卡）
        App.addServiceStateListener(this, true)
        _refreshAll()
        // 通过广播主动向 system_server 查询系统 MAC（替代旧 YukiHookDataChannel）
        _requestSystemMac()
    }

    override fun onPause() {
        super.onPause()
        App.removeServiceStateListener(this)
        _binding?.let { requireContext().unregisterReceiver(macReceiver) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** XposedService 绑定状态变化回调（激活/失活即时刷新 UI） */
    override fun onServiceStateChanged(active: Boolean) {
        if (view == null) return
        _refreshAll()
        if (active) _requestSystemMac()
    }

    /**
     * 向 system_server 主动请求当前系统 MAC（发送 ACTION_QUERY_MAC 广播，
     * system_server 侧接收后回发 ACTION_MAC_DETECTED）。
     * 模块未激活/Hook 未安装时静默忽略（不影响其它功能）。
     */
    private fun _requestSystemMac() {
        runCatching {
            requireContext().sendBroadcast(Intent(WifiServiceHooker.ACTION_QUERY_MAC))
        }.onFailure { /* 无广播权限等异常忽略 */ }
    }

    private fun _refreshAll() {
        updatingUI = true
        _updateStatusCard()
        _refreshDeviceMac()
        _refreshActiveMac()
        binding.hookSwitch.isChecked = PrefManager.isHookOn(requireContext())
        val saved = PrefManager.getCustomMac(requireContext())
        if (saved.isNotEmpty() && binding.edittextNewMac.text.isNullOrEmpty()) {
            binding.edittextNewMac.setText(saved)
        }
        updatingUI = false
    }

    /**
     * 判断模块是否已在 Xposed/LSPosed 中激活：
     * 现代 API 下以“模块 App 是否收到框架推送的 XposedService”为准
     * （框架只在模块处于激活环境时推送服务），重启后立即显示正确状态。
     */
    private fun _isModuleActive(): Boolean = App.isModuleActive()

    private fun _updateStatusCard() {
        val moduleActive = _isModuleActive()
        // 服务已推送但作用域未包含 system_server 虚拟包名 “system” 时，
        // 模块虽“启用”但 Hook 不会运行 —— 单独给出告警分支
        val service = App.currentService()
        val scopeHasSystem = service?.scope?.contains("system") == true
        val hookOn = moduleActive && scopeHasSystem && PrefManager.isHookOn(requireContext())

        when {
            !moduleActive -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_status_inactive_24)
                binding.moduleStatus.text = getString(R.string.status_inactive)
                binding.serviceStatus.text = getString(R.string.status_detail_inactive)
                binding.statusMac.visibility = View.GONE
                // 未激活：给出可操作的启用指引
                binding.statusExtra.text = getString(R.string.status_extra_activation_hint)
            }
            !scopeHasSystem -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_status_hookoff_24)
                binding.moduleStatus.text = getString(R.string.status_title_scope_missing)
                binding.serviceStatus.text = getString(R.string.status_detail_scope_missing)
                binding.statusMac.visibility = View.GONE
                binding.statusExtra.text = _buildFrameworkInfo()
            }
            !hookOn -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_status_hookoff_24)
                binding.moduleStatus.text = getString(R.string.status_activated)
                binding.serviceStatus.text = getString(R.string.status_detail_hook_off)
                binding.statusMac.visibility = View.GONE
                binding.statusExtra.text = getString(R.string.status_extra_hook_off_hint)
            }
            else -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_status_active_24)
                binding.moduleStatus.text = getString(R.string.status_activated)
                // 副标题：仅说明状态；MAC 地址放到下面一行更小的等宽小字里
                val customMac = PrefManager.getCustomMac(requireContext())
                val systemMac = _getSystemMacDisplay()
                if (customMac.isNotEmpty()) {
                    binding.serviceStatus.text = getString(R.string.status_active_custom)
                    binding.statusMac.text = getString(R.string.status_mac_details, customMac, systemMac)
                } else {
                    binding.serviceStatus.text = getString(R.string.status_active_system)
                    binding.statusMac.text = getString(R.string.status_mac_system_only, systemMac)
                }
                binding.statusMac.visibility = View.VISIBLE
                // 扩展行：框架 / API / 作用域 / 远程偏好通道能力
                binding.statusExtra.text = _buildFrameworkInfo()
            }
        }
    }

    /**
     * 从 XposedService 组装框架信息（框架名与版本、Xposed API 版本、
     * 当前作用域、远程偏好能力）。框架未绑定或读取失败时返回空串（隐藏该行）。
     */
    private fun _buildFrameworkInfo(): String {
        val service = App.currentService() ?: return ""
        return runCatching {
            val scope = service.scope
            val scopeText = if (scope.isEmpty()) {
                getString(R.string.status_extra_scope_empty)
            } else {
                scope.joinToString(", ")
            }
            val remoteOk = (service.frameworkProperties and XposedService.PROP_CAP_REMOTE) != 0L
            val remoteText = if (remoteOk) {
                getString(R.string.status_extra_remote_ok)
            } else {
                getString(R.string.status_extra_remote_na)
            }
            getString(
                R.string.status_extra_framework,
                service.frameworkName,
                service.frameworkVersion,
                service.apiVersion,
                scopeText,
                remoteText
            )
        }.getOrDefault("")
    }

    /** 读取本地缓存的系统 MAC（出厂 MAC，由主动拉取/广播更新），不可用返回“未知” */
    private fun _getSystemMacDisplay(): String {
        val localPrefs = requireContext().getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        return localPrefs.getString("deviceMac", null) ?: getString(R.string.mac_not_set)
    }

    private fun _setupToggles() {
        binding.hookSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingUI) return@setOnCheckedChangeListener
            PrefManager.setHookState(requireContext(), checked)
            _updateStatusCard()
            // 零点击：开关切换即同步配置并应用（无需再点“应用 MAC”）
            requireContext().sendBroadcast(Intent(WifiServiceHooker.ACTION_CONFIG_CHANGED))
        }
    }

    private fun _refreshDeviceMac() {
        val localPrefs = requireContext().getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val mac = localPrefs.getString("deviceMac", null)
        binding.textviewDeviceMac.text = mac ?: getString(R.string.mac_not_set)
    }

    private fun _refreshActiveMac() {
        val saved = PrefManager.getCustomMac(requireContext())
        if (saved.isNotEmpty()) {
            binding.textviewCurrentMac.text = saved
        } else {
            // 未设置自定义 MAC 时，显示系统 MAC（本地缓存），避免显示“未知”
            val localPrefs = requireContext().getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            val deviceMac = localPrefs.getString("deviceMac", null)
            binding.textviewCurrentMac.text = deviceMac ?: getString(R.string.mac_not_set)
        }
    }

    private fun _setupMacCard() {
        val editText = binding.edittextNewMac
        editText.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(17))
        editText.addTextChangedListener(MacTextWatcher())

        binding.btnGenerateMac.setOnClickListener {
            editText.setText(MacUtils.generateRandom())
        }

        binding.btnSetMac.setOnClickListener {
            val mac = editText.text.toString().uppercase()
            when (MacUtils.validate(mac)) {
                MacUtils.ValidationResult.BAD_LENGTH ->
                    _showError(getString(R.string.error_bad_length))
                MacUtils.ValidationResult.ALL_ZEROS ->
                    _showError(getString(R.string.error_all_zeros))
                MacUtils.ValidationResult.ODD_FIRST_OCTET ->
                    _showError(getString(R.string.error_odd_first_octet))
                MacUtils.ValidationResult.VALID -> {
                    PrefManager.setCustomMac(requireContext(), mac)
                    binding.textviewCurrentMac.text = mac
                    _applyMac()
                }
            }
        }
    }

    private fun _applyMac() {
        // 广播携带目标 MAC（由 system_server 接收并立即应用，
        // 不依赖跨进程 prefs 读取时序，确保首次安装后第一次点击即生效）
        val mac = PrefManager.getCustomMac(requireContext())
        requireContext().sendBroadcast(
            Intent(WifiServiceHooker.ACTION_APPLY_MAC)
                .putExtra(WifiServiceHooker.EXTRA_MAC, mac)
        )
        // 零点击：同步最新配置（如 AP 覆写开时一并应用 AP 接口）
        requireContext().sendBroadcast(Intent(WifiServiceHooker.ACTION_CONFIG_CHANGED))
        Snackbar.make(binding.root, R.string.mac_set_success, Snackbar.LENGTH_LONG)
            .setAction(R.string.open_wifi_settings) {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            .show()
    }

    private fun _showError(msg: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
