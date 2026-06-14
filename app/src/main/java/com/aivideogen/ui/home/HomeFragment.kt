package com.aivideogen.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aivideogen.R
import com.aivideogen.databinding.FragmentHomeBinding
import com.aivideogen.viewmodel.VideoGeneratorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoGeneratorViewModel by activityViewModels()
    private lateinit var recentAdapter: RecentProjectsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
    }

    private fun setupUI() {
        recentAdapter = RecentProjectsAdapter(
            onItemClick = { project ->
                if (project.outputVideoPath != null) {
                    val action = HomeFragmentDirections.actionHomeToGallery()
                    findNavController().navigate(action)
                }
            },
            onDeleteClick = { project ->
                viewModel.deleteProject(project)
            }
        )

        binding.rvRecentProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentAdapter
        }

        binding.fabCreate.setOnClickListener {
            viewModel.resetState()
            findNavController().navigate(R.id.action_home_to_generate)
        }

        binding.btnQuickStart.setOnClickListener {
            viewModel.resetState()
            findNavController().navigate(R.id.action_home_to_generate)
        }

        binding.cardTextToVideo.setOnClickListener {
            viewModel.resetState()
            findNavController().navigate(R.id.action_home_to_generate)
        }

        binding.cardImageToVideo.setOnClickListener {
            viewModel.resetState()
            findNavController().navigate(R.id.action_home_to_generate)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allProjects.collect { projects ->
                    recentAdapter.submitList(projects.take(10))
                    binding.emptyState.visibility =
                        if (projects.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvRecentProjects.visibility =
                        if (projects.isEmpty()) View.GONE else View.VISIBLE

                    binding.tvTotalVideos.text = projects.count {
                        it.status.name == "COMPLETED"
                    }.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
