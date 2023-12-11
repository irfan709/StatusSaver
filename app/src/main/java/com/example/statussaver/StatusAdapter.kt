package com.example.statussaver

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StatusAdapter(
    private val context: Context,
    private var statusList: ArrayList<StatusModel>,
    private var clickLIstener: (StatusModel) -> Unit
): RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    class StatusViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imagestatus: ImageView = itemView.findViewById(R.id.imagestatus)
        val videcard: CardView = itemView.findViewById(R.id.videocard)
        val videoicon: ImageView = itemView.findViewById(R.id.videoicon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_status, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return statusList.size
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        if (statusList[position].fileUri.endsWith(".mp4")) {
            holder.videcard.visibility = View.VISIBLE
            holder.videoicon.visibility = View.VISIBLE
        } else {
            holder.videcard.visibility = View.GONE
            holder.videoicon.visibility = View.GONE
        }

        Glide.with(context).load(Uri.parse(statusList[position].fileUri)).into(holder.imagestatus)

        holder.imagestatus.setOnClickListener {
            clickLIstener(statusList[position])
        }
    }

}