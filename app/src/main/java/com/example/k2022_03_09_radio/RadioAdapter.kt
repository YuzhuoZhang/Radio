package com.example.k2022_03_09_radio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RadioAdapter(
    private val stations: MutableList<RadioStation>,
    private val onItemClick: (RadioStation) -> Unit
) : RecyclerView.Adapter<RadioAdapter.ViewHolder>() {

    private var isLoading = false
    private var loadMoreListener: (() -> Unit)? = null

    fun setLoadMoreListener(listener: () -> Unit) {
        this.loadMoreListener = listener
    }

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_radio_station, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == stations.size - 1 && isLoading) {
            loadMoreListener?.invoke()
        }

        val station = stations[position]
        holder.bind(station)
        holder.itemView.setOnClickListener { onItemClick(station) }
    }

    override fun getItemCount(): Int {
        return stations.size
    }

    fun addItems(newItems: List<RadioStation>) {
        val startPosition = stations.size
        stations.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    fun clearItems() {
        stations.clear()
        notifyDataSetChanged()
    }

    // ViewHolder class
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(station: RadioStation) {
            // Bind station data to views
            itemView.findViewById<TextView>(R.id.stationNameTextView).text = station.name
            // Load station image using Glide or Picasso
            // itemView.findViewById<ImageView>(R.id.stationImageView).load(station.imageResource)
        }
    }
}


