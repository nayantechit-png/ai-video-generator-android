package com.aivideogen.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aivideogen.databinding.FragmentSettingsBinding
import com.aivideogen.viewmodel.VideoGeneratorViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VideoGeneratorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAppVersion.text = "v${requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName}"

        binding.btnSaveKeys.setOnClickListener {
            val stability = binding.etStabilityKey.text.toString().trim()
            val openai    = binding.etOpenaiKey.text.toString().trim()
            viewModel.setApiKeys(stability, openai)
            Snackbar.make(binding.root, "API keys saved for this session", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnClearCache.setOnClickListener {
            com.aivideogen.utils.FileUtils.cleanTempFiles(requireContext())
            Snackbar.make(binding.root, "Cache cleared", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
