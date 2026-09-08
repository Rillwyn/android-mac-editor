package io.github.Rillwyn.androidmaceditor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.Rillwyn.androidmaceditor.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "") ?: ""
        val locale = if (lang.isNotEmpty()) Locale.forLanguageTag(lang) else Locale.getDefault()
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val prefs = getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            val lang = prefs.getString("language", "") ?: ""
            val locale = if (lang.isNotEmpty()) Locale.forLanguageTag(lang) else Locale.getDefault()
            overrideConfiguration.setLocale(locale)
            overrideConfiguration.setLayoutDirection(locale)
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "") ?: ""
        val locale = if (lang.isNotEmpty()) Locale.forLanguageTag(lang) else Locale.getDefault()
        val isRtl = android.text.TextUtils.getLayoutDirectionFromLocale(locale) == android.view.View.LAYOUT_DIRECTION_RTL
        val layoutDir = if (isRtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR
        window.decorView.layoutDirection = layoutDir

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.layoutDirection = layoutDir
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.viewPager.adapter = MainPagerAdapter(this)
        // 关闭预加载，页面随滑动/点击即时创建（Fragment 状态由 FragmentStateAdapter 保存）
        binding.viewPager.offscreenPageLimit = 1

        // 底部导航与 ViewPager2 双向联动
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.setCurrentItem(0, false)
                R.id.nav_settings -> binding.viewPager.setCurrentItem(1, false)
                R.id.nav_about -> binding.viewPager.setCurrentItem(2, false)
                else -> return@setOnItemSelectedListener false
            }
            true
        }
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                _syncNavSelection(position)
                _updateToolbarTitle(position)
            }
        })

        // 恢复语言切换前的所在页面（设置页切换语言时写入 savedTab）
        val savedTab = getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, MODE_PRIVATE)
            .getInt("savedTab", 0)
        _restoreTab(savedTab)
    }

    /** 语言切换（recreate）后恢复原所在页面 */
    private fun _restoreTab(savedTab: Int) {
        if (savedTab in 0..2) {
            binding.viewPager.setCurrentItem(savedTab, false)
        }
    }

    private fun _syncNavSelection(position: Int) {
        val menuItem = when (position) {
            0 -> R.id.nav_home
            1 -> R.id.nav_settings
            else -> R.id.nav_about
        }
        binding.bottomNav.menu.findItem(menuItem)?.isChecked = true
        // 同步记录当前页面，供语言切换后恢复
        getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, MODE_PRIVATE)
            .edit().putInt("savedTab", position).apply()
    }

    /** 工具栏标题随页面切换（主页显示 App 名） */
    private fun _updateToolbarTitle(position: Int) {
        val title = when (position) {
            1 -> getString(R.string.toolbar_settings)
            2 -> getString(R.string.toolbar_about)
            else -> getString(R.string.app_name)
        }
        binding.toolbar.title = title
    }

    /** 三个页面的 FragmentStateAdapter */
    inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                1 -> SettingsFragment()
                2 -> AboutFragment()
                else -> HomeFragment()
            }
        }
    }
}
