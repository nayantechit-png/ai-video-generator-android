package com.aivideogen.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.aivideogen.data.model.VideoProject
import com.aivideogen.databinding.FragmentGalleryBinding
import com.aivideogen.viewmodel.VideoGeneratorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoGeneratorViewModel by activityViewModels()
    private lateinit var galleryAdapter: GalleryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(
            onItemClick = { project -> openVideoPlayer(project) },
            onShareClick = { project -> shareVideo(project) },
            onDeleteClick = { project -> confirmDelete(project) }
        )

        binding.rvGallery.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = galleryAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedProjects.collect { projects ->
                    galleryAdapter.submitList(projects)
                    binding.emptyGallery.visibility =
                        if (projects.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvGallery.visibility =
                        if (projects.isEmpty()) View.GONE else View.VISIBLE
                    binding.tvVideoCount.text = "${projects.size} video(s)"
                }
            }
        }
    }

    private fun openVideoPlayer(project: VideoProject) {
        project.outputVideoPath?.let { path ->
            val dialog = VideoPlayerDialog.newInstance(path, project.title.ifBlank { "AI Video" })
            dialog.show(childFragmentManager, "video_player")
        }
    }

    private fun shareVideo(project: VideoProject) {
        project.outputVideoPath?.let { path ->
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                java.io.File(path)
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "AI Generated Video")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Share Video"))
        }
    }

    private fun confirmDelete(project: VideoProject) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Video")
            .setMessage("Delete \"${project.title.ifBlank { "this video" }}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteProject(project) }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
