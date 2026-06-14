package com.aivideogen.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aivideogen.data.model.VideoProject
import com.aivideogen.databinding.ItemGalleryVideoBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryAdapter(
    private val onItemClick: (VideoProject) -> Unit,
    private val onShareClick: (VideoProject) -> Unit,
    private val onDeleteClick: (VideoProject) -> Unit
) : ListAdapter<VideoProject, GalleryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemGalleryVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: VideoProject) {
            binding.tvTitle.text = project.title.ifBlank {
                project.prompt.take(30).ifBlank { "Untitled" }
            }
            binding.tvDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Date(project.createdAt))
            binding.tvResolution.text = project.resolution.displayName
            binding.tvDuration.text = "${project.duration}s"

            // Load thumbnail
            val thumb = project.thumbnailPath ?: project.imagePaths.firstOrNull()
            Glide.with(binding.ivThumbnail)
                .load(thumb)
                .placeholder(com.aivideogen.R.drawable.ic_video_placeholder)
                .centerCrop()
                .into(binding.ivThumbnail)

            binding.root.setOnClickListener { onItemClick(project) }
            binding.btnShare.setOnClickListener { onShareClick(project) }
            binding.btnDelete.setOnClickListener { onDeleteClick(project) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryVideoBinding.inflate(
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
