package com.aivideogen.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.aivideogen.R
import com.aivideogen.databinding.DialogVideoPlayerBinding

class VideoPlayerDialog : DialogFragment() {

    private var _binding: DialogVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val videoPath = arguments?.getString(ARG_VIDEO_PATH) ?: return
        val title = arguments?.getString(ARG_TITLE) ?: "Video"

        binding.tvTitle.text = title
        binding.btnClose.setOnClickListener { dismiss() }

        initPlayer(videoPath)
    }

    private fun initPlayer(path: String) {
        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(path))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }

    companion object {
        private const val ARG_VIDEO_PATH = "video_path"
        private const val ARG_TITLE = "title"

        fun newInstance(videoPath: String, title: String) = VideoPlayerDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_VIDEO_PATH, videoPath)
                putString(ARG_TITLE, title)
            }
        }
    }
}
