package com.aivideogen.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aivideogen.data.model.GenerationStatus
import com.aivideogen.data.model.VideoProject
import com.aivideogen.databinding.ItemRecentProjectBinding
import com.bumptech.glide.Glide

class RecentProjectsAdapter(
    private val onItemClick: (VideoProject) -> Unit,
    private val onDeleteClick: (VideoProject) -> Unit
) : ListAdapter<VideoProject, RecentProjectsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemRecentProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: VideoProject) {
            binding.tvTitle.text = project.title.ifBlank {
                project.prompt.take(40).ifBlank { "Untitled" }
            }
            binding.tvPrompt.text = project.prompt.take(60)
            binding.tvStatus.text = project.status.name

            // Status color
            binding.tvStatus.setTextColor(
                when (project.status) {
                    GenerationStatus.COMPLETED -> 0xFF4CAF50.toInt()
                    GenerationStatus.FAILED    -> 0xFFF44336.toInt()
                    GenerationStatus.PROCESSING -> 0xFFFF9800.toInt()
                    else -> 0xFF9E9E9E.toInt()
                }
            )

            // Thumbnail
            val thumbPath = project.thumbnailPath ?: project.imagePaths.firstOrNull()
            if (thumbPath != null) {
                Glide.with(binding.ivThumbnail)
                    .load(thumbPath)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(
                    com.aivideogen.R.drawable.ic_video_placeholder
                )
            }

            // Progress bar for in-progress items
            if (project.status == GenerationStatus.PROCESSING) {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.progressBar.progress = project.progress
            } else {
                binding.progressBar.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(project) }
            binding.btnDelete.setOnClickListener { onDeleteClick(project) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentProjectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<VideoProject>() {
        override fun areItemsTheSame(a: VideoProject, b: VideoProject) = a.id == b.id
        override fun areContentsTheSame(a: VideoProject, b: VideoProject) = a == b
    }
}
