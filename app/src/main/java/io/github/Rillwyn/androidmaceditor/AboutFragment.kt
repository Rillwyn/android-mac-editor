package io.github.Rillwyn.androidmaceditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.Rillwyn.androidmaceditor.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 版本号从 BuildConfig 自动读取，避免硬编码
        binding.aboutVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)

        binding.thisProjectUrl.text = getString(R.string.about_this_project_url)
        binding.originalProjectUrl.text = getString(R.string.about_original_project_url)

        binding.linkThisProject.setOnClickListener {
            openUrl(getString(R.string.about_this_project_url))
        }
        binding.linkOriginalProject.setOnClickListener {
            openUrl(getString(R.string.about_original_project_url))
        }

        // 贡献者：两级折叠（贡献者列表 -> 每人按版本做了什么的说明）
        binding.rillwynNotesText.text =
            resources.getStringArray(R.array.about_contrib_rillwyn).joinToString("\n")
        binding.amrNotesText.text =
            resources.getStringArray(R.array.about_contrib_amr).joinToString("\n")
        binding.toggleContributors.setOnClickListener {
            toggleArea(binding.contributorsList, binding.contributorsCaret)
        }
        binding.toggleRillwyn.setOnClickListener {
            toggleArea(binding.rillwynNotesBox, binding.rillwynCaret)
        }
        binding.toggleAmr.setOnClickListener {
            toggleArea(binding.amrNotesBox, binding.amrCaret)
        }
    }

    /** 展开/收起区域并切换箭头符号，展开时请求滚动使内容完整可见 */
    private fun toggleArea(area: View, caret: android.widget.TextView) {
        val expanded = area.visibility == View.GONE
        area.visibility = if (expanded) View.VISIBLE else View.GONE
        caret.text = if (expanded) "▾" else "▸"
        if (expanded) {
            area.post {
                val rect = android.graphics.Rect(0, 0, area.width, area.height)
                area.requestRectangleOnScreen(rect, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
