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
