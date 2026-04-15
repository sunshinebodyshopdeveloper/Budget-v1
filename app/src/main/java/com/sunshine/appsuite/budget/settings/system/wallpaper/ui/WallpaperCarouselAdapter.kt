package com.sunshine.appsuite.budget.settings.system.wallpaper.ui

import android.R
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sunshine.appsuite.budget.databinding.ItemWallpaperBinding

class WallpaperCarouselAdapter(
    private val onWallpaperSelected: (String) -> Unit,
    private val onInitialImageResolved: () -> Unit
) : RecyclerView.Adapter<WallpaperCarouselAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()
    private var didResolveInitialImage = false

    inner class ViewHolder(
        private val binding: ItemWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val imageView: ImageView = binding.imageViewItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("WallpaperAdapter", "onCreateViewHolder")
        val binding = ItemWallpaperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = items[position]
        Log.d("WallpaperAdapter", "onBindViewHolder position=$position url=$url")

        holder.imageView.load(url) {
            crossfade(true)
            allowHardware(false)
            placeholder(ColorDrawable(0xFFEAEAEA.toInt()))
            error(R.drawable.ic_menu_report_image)
            listener(
                onStart = {
                    Log.d("WallpaperAdapter", "Coil start position=$position")
                },
                onSuccess = { _, _ ->
                    Log.d("WallpaperAdapter", "Coil success position=$position")
                    notifyInitialImageResolvedOnce()
                },
                onError = { _, result ->
                    Log.e("WallpaperAdapter", "Coil error position=$position", result.throwable)
                    notifyInitialImageResolvedOnce()
                }
            )
        }

        holder.itemView.setOnClickListener {
            onWallpaperSelected(url)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        Log.d("WallpaperAdapter", "submitList size=${items.size}")
        notifyDataSetChanged()
    }

    fun resetInitialLoadState() {
        didResolveInitialImage = false
    }

    private fun notifyInitialImageResolvedOnce() {
        if (didResolveInitialImage) return
        didResolveInitialImage = true
        onInitialImageResolved.invoke()
    }
}