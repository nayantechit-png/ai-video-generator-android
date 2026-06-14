package com.aivideogen.ui.generate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aivideogen.R
import com.aivideogen.data.model.*
import com.aivideogen.databinding.FragmentGenerateBinding
import com.aivideogen.utils.FileUtils
import com.aivideogen.viewmodel.VideoGeneratorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GenerateFragment : Fragment() {

    private var _binding: FragmentGenerateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoGeneratorViewModel by activityViewModels()
    private lateinit var imagePickerAdapter: ImagePickerAdapter

    // Image picker launcher
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val file = FileUtils.copyUriToFile(requireContext(), uri)
            file?.let { viewModel.addImagePath(it.absolutePath) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImagePicker()
        setupSpinners()
        setupSliders()
        setupButtons()
        observeState()
    }

    private fun setupImagePicker() {
        imagePickerAdapter = ImagePickerAdapter(
            onRemoveClick = { path -> viewModel.removeImagePath(path) }
        )
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = imagePickerAdapter
        }
        binding.btnAddImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }
    }

    private fun setupSpinners() {
        // Style spinner
        val styles = VideoStyle.values().map { it.displayName }
        val styleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, styles)
        styleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStyle.adapter = styleAdapter
        binding.spinnerStyle.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: View?, pos: Int, id: Long) {
                viewModel.setStyle(VideoStyle.values()[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // Resolution spinner
        val resolutions = VideoResolution.values().map { it.displayName }
        val resAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resolutions)
        resAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerResolution.adapter = resAdapter
        binding.spinnerResolution.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: View?, pos: Int, id: Long) {
                viewModel.setResolution(VideoResolution.values()[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // Duration options
        val durations = listOf(3, 5, 8, 10, 15, 30)
        val durAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            durations.map { "$it sec" })
        durAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuration.adapter = durAdapter
        binding.spinnerDuration.setSelection(1) // default 5s
        binding.spinnerDuration.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: View?, pos: Int, id: Long) {
                viewModel.setDuration(durations[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // AI Provider
        val providers = AIProvider.values().map { it.displayName }
        val providerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, providers)
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProvider.adapter = providerAdapter
        binding.spinnerProvider.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: View?, pos: Int, id: Long) {
                viewModel.setAIProvider(AIProvider.values()[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun setupSliders() {
        binding.sliderMotion.addOnChangeListener { _, value, _ ->
            viewModel.setMotionStrength(value)
            binding.tvMotionValue.text = "%.1f".format(value)
        }
        binding.sliderCfg.addOnChangeListener { _, value, _ ->
            viewModel.setCfgScale(value)
            binding.tvCfgValue.text = "%.1f".format(value)
        }
    }

    private fun setupButtons() {
        binding.etPrompt.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setPrompt(s?.toString() ?: "")
                binding.tvCharCount.text = "${s?.length ?: 0}/500"
            }
        })

        binding.etNegativePrompt.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setNegativePrompt(s?.toString() ?: "")
            }
        })

        binding.btnGenerate.setOnClickListener {
            if (binding.etApiKey.text.isNotBlank()) {
                viewModel.setApiKeys(
                    stability = binding.etApiKey.text.toString(),
                    openAi = binding.etApiKeyOpenai.text.toString()
                )
            }
            viewModel.startGeneration()
        }

        binding.btnCancel.setOnClickListener {
            showCancelDialog()
        }

        binding.btnClearImages.setOnClickListener {
            viewModel.clearImages()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedImagePaths.collect { paths ->
                        imagePickerAdapter.submitList(paths)
                        binding.tvImageCount.text = "${paths.size} image(s) selected"
                        binding.btnClearImages.visibility =
                            if (paths.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.generationState.collect { state ->
                        when (state) {
                            is GenerationState.Idle -> showIdleUI()
                            is GenerationState.Progress -> showProgressUI(state.percent, state.message)
                            is GenerationState.Success -> showSuccessUI()
                            is GenerationState.Error   -> showErrorUI(state.message)
                            is GenerationState.Cancelled -> showIdleUI()
                        }
                    }
                }
            }
        }
    }

    private fun showIdleUI() {
        binding.generationProgress.visibility = View.GONE
        binding.btnGenerate.isEnabled = true
        binding.btnCancel.visibility = View.GONE
    }

    private fun showProgressUI(percent: Int, message: String) {
        binding.generationProgress.visibility = View.VISIBLE
        binding.progressBar.progress = percent
        binding.tvProgressMessage.text = message
        binding.tvProgressPercent.text = "$percent%"
        binding.btnGenerate.isEnabled = false
        binding.btnCancel.visibility = View.VISIBLE
    }

    private fun showSuccessUI() {
        binding.generationProgress.visibility = View.GONE
        binding.btnGenerate.isEnabled = true
        binding.btnCancel.visibility = View.GONE
        Snackbar.make(binding.root, "✅ Video generated successfully!", Snackbar.LENGTH_LONG)
            .setAction("View") {
                findNavController().navigate(R.id.action_generate_to_gallery)
            }.show()
        viewModel.resetState()
        findNavController().navigate(R.id.action_generate_to_gallery)
    }

    private fun showErrorUI(message: String) {
        binding.generationProgress.visibility = View.GONE
        binding.btnGenerate.isEnabled = true
        binding.btnCancel.visibility = View.GONE
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Generation Failed")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showCancelDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Generation?")
            .setMessage("The current generation will be stopped.")
            .setPositiveButton("Cancel Generation") { _, _ -> viewModel.cancelGeneration() }
            .setNegativeButton("Keep Going") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
