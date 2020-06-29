package com.example.vani.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vani.R
import com.example.vani.databinding.VideoItemBinding
import com.example.vani.ui.pojos.Video

class VideoListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mContext: Context? = null
    private val videoList = mutableListOf<Video>()
    private lateinit var binding: VideoItemBinding
    private lateinit var callbackToVideoListFragment: CallbackToVideoListFragment

    fun setCallBacktoVideoListFragment(callbackToVideoListFragment: CallbackToVideoListFragment) {
        this.callbackToVideoListFragment = callbackToVideoListFragment
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(mContext)
        binding = VideoItemBinding.inflate(layoutInflater, parent, false)
        return VideoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VideoViewHolder -> holder.bind(videoList[position])
        }
    }

    inner class VideoViewHolder(val binding: VideoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Video) {

            binding.name.text = item.name
            Glide.with(binding.root)
                .load(item.uri)
                .error(R.drawable.ic_airplay_black_24dp)
                .into(binding.thumbnail)


           // val bundle = bundleOf("uri" to item.uri.toString())

            binding.thumbnail.setOnClickListener {
                callbackToVideoListFragment.sendDataAndOpenPlayer(item)
            }

        }
    }

    fun updateList(list: MutableList<Video>?) {
        videoList.clear()
        list?.let { videoList.addAll(it) }
        notifyDataSetChanged()
    }

    interface CallbackToVideoListFragment {
        fun sendDataAndOpenPlayer(uri: Video)
    }
}


