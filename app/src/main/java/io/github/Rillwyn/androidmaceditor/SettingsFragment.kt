package io.github.Rillwyn.androidmaceditor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
        _setupLanguageDropdown()
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

    /** 语言选项：English / 中文 / العربية */
    private data class LangOption(val code: String, val label: String)

    private fun languageOptions(): List<LangOption> = listOf(
        LangOption("en", getString(R.string.language_english)),
        LangOption("zh", getString(R.string.language_chinese)),
        LangOption("ar", getString(R.string.language_arabic))
    )

    /** 语言下拉框：选择后保存并重建 Activity（恢复原所在页面） */
    private fun _setupLanguageDropdown() {
        val options = languageOptions()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            options.map { it.label }
        )
        binding.languageDropdown.setAdapter(adapter)
        binding.languageDropdown.setOnItemClickListener { _, _, position, _ ->
            if (updatingUI) return@setOnItemClickListener
            applyLanguage(options[position].code)
        }
    }

    private fun applyLanguage(lang: String) {
        // 记住当前 tab（设置页 index = 1），重启后由 MainActivity 恢复
        requireContext().getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("language", lang)
            .putInt("savedTab", 1)
            .apply()

        // 同步应用 AppCompatDelegate 全局语言/布局方向
        val appLocale = if (lang.isNotEmpty()) {
            androidx.core.os.LocaleListCompat.forLanguageTags(lang)
        } else {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)

        // 无动画平滑重启 Activity，确保 RTL/LTR 布局方向与资源立刻生效而无需手动杀死重开
        val activity = requireActivity()
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        activity.finish()
        activity.startActivity(intent)
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
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
        val code = if (currentLang.isNotEmpty()) {
            currentLang
        } else {
            val sysLang = resources.configuration.locales[0].language
            when {
                sysLang.startsWith("zh") -> "zh"
                sysLang.startsWith("ar") -> "ar"
                else -> "en"
            }
        }
        val label = languageOptions().firstOrNull { it.code == code }?.label
            ?: getString(R.string.language_english)
        binding.languageDropdown.setText(label, false)
    }
}
