package io.github.Rillwyn.androidmaceditor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.Rillwyn.androidmaceditor.databinding.FragmentSettingsBinding
import io.github.Rillwyn.androidmaceditor.hookers.WifiServiceHooker
import io.github.Rillwyn.androidmaceditor.utils.PrefManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var updatingUI = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _setupLanguageToggle()
        _setupSwitches()
        _refreshAll()
    }

    override fun onResume() {
        super.onResume()
        _refreshAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** 语言行内单选（English / 中文 / العربية）：点击即保存并重建 Activity（恢复原所在页面） */
    private fun _setupLanguageToggle() {
        binding.languageToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (updatingUI || !isChecked) return@addOnButtonCheckedListener
            val lang = when (checkedId) {
                R.id.language_zh -> "zh"
                R.id.language_ar -> "ar"
                else -> "en"
            }
            // 记住当前 tab（设置页 index = 1），recreate 后由 MainActivity 恢复
            requireContext().getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("language", lang)
                .putInt("savedTab", 1)
                .apply()
            requireActivity().recreate()
        }
    }

    private fun _setupSwitches() {
        binding.forceRandomizationSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingUI) return@setOnCheckedChangeListener
            PrefManager.setForceShowMacRandomization(requireContext(), checked)
            // 零点击：立即广播让 system_server 同步最新配置
            requireContext().sendBroadcast(Intent(WifiServiceHooker.ACTION_CONFIG_CHANGED))
        }
        binding.apMacOverrideSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingUI) return@setOnCheckedChangeListener
            PrefManager.setApMacOverride(requireContext(), checked)
            // 零点击：AP 覆写开关变化时立即应用/同步
            requireContext().sendBroadcast(Intent(WifiServiceHooker.ACTION_CONFIG_CHANGED))
        }
    }

    private fun _refreshAll() {
        updatingUI = true
        _refreshLanguageToggle()
        binding.forceRandomizationSwitch.isChecked = PrefManager.isForceShowMacRandomization(requireContext())
        binding.apMacOverrideSwitch.isChecked = PrefManager.isApMacOverride(requireContext())
        updatingUI = false
    }

    private fun _refreshLanguageToggle() {
        val prefs = requireContext().getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val currentLang = prefs.getString("language", "") ?: ""
        val checkedId = when (currentLang) {
            "en" -> R.id.language_en
            "zh" -> R.id.language_zh
            "ar" -> R.id.language_ar
            else -> {
                val sysLang = resources.configuration.locales[0].language
                when {
                    sysLang.startsWith("zh") -> R.id.language_zh
                    sysLang.startsWith("ar") -> R.id.language_ar
                    else -> R.id.language_en
                }
            }
        }
        binding.languageToggle.check(checkedId)
    }
}
