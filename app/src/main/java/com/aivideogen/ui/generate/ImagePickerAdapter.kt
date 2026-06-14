package com.aivideogen.ui.generate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aivideogen.databinding.ItemImagePickerBinding
import com.bumptech.glide.Glide

class ImagePickerAdapter(
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<String, ImagePickerAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemImagePickerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(path: String) {
            Glide.with(binding.ivImage)
                .load(path)
                .centerCrop()
                .into(binding.ivImage)

            binding.btnRemove.setOnClickListener { onRemoveClick(path) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImagePickerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(a: String, b: String) = a == b
        override fun areContentsTheSame(a: String, b: String) = a == b
    }
}
