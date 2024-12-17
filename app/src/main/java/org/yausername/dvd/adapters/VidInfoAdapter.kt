package org.yausername.dvd.adapters

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yausername.youtubedl_android.mapper.VideoInfo
//import kotlinx.android.synthetic.main.vid_format.view.*
//import kotlinx.android.synthetic.main.vid_header.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.yausername.dvd.R
import org.yausername.dvd.databinding.VidFormatBinding
import org.yausername.dvd.databinding.VidHeaderBinding
import org.yausername.dvd.model.VidInfoItem
import org.yausername.dvd.utils.NumberUtils
import java.util.concurrent.TimeUnit

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class VidInfoAdapter(private val clickListener: VidInfoListener) :
    ListAdapter<VidInfoItem, RecyclerView.ViewHolder>(
        VidInfoDiffCallback()
    ) {


    private lateinit var binding1: VidHeaderBinding
    private lateinit var binding2: VidFormatBinding
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    fun fill(vidInfo: VideoInfo?) {
        adapterScope.launch {
            if (vidInfo == null) {
                submitList(emptyList())
                return@launch
            }
            val items = mutableListOf<VidInfoItem>()
            withContext(Dispatchers.Default) {
                items.add(VidInfoItem.VidHeaderItem(vidInfo))
                vidInfo.formats?.forEach { format ->
                    items.add(
                        VidInfoItem.VidFormatItem(
                            vidInfo,
                            format.formatId!!
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                submitList(items.toList())
            }
        }
    }
    inner class ViewHolder(private val binding:VidFormatBinding) : RecyclerView.ViewHolder(binding.root) {
        val format_tv: TextView = itemView.findViewById(R.id.format_tv)
        val ext_tv: TextView = itemView.findViewById(R.id.ext_tv)
        val size_tv: TextView = itemView.findViewById(R.id.size_tv)
        val fps_tv: TextView = itemView.findViewById(R.id.fps_tv)
        val abr_tv: TextView = itemView.findViewById(R.id.abr_tv)
        val format_ic: ImageView = itemView.findViewById(R.id.format_ic)
        val itemshare: ImageView = itemView.findViewById(R.id.item_share)
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val vidItem = getItem(position) as VidInfoItem.VidFormatItem
                val vidFormat = vidItem.vidFormat
                with(holder.itemView) {
                    binding2.formatTv.text = vidFormat.format
                    binding2.extTv.text = vidFormat.ext
                    binding2.sizeTv.text = Formatter.formatShortFileSize(context, vidFormat.fileSize)
                    binding2.fpsTv.text = context.getString(R.string.fps_value, vidFormat.fps)
                    binding2.abrTv.text = context.getString(R.string.abr_value, vidFormat.abr)
                    if (vidFormat.acodec != "none" && vidFormat.vcodec == "none") {
                        binding2.formatIc.setImageResource(R.drawable.ic_baseline_audiotrack_24)
                    } else {
                        binding2.formatIc.setImageResource(R.drawable.ic_baseline_video_library_24)
                    }
                    binding2.itemShare.setOnClickListener {
                        shareUrl(vidFormat.url, context)
                    }
                    setOnClickListener { clickListener.onClick(vidItem) }
                }
            }
            else -> {
                val vidItem = getItem(position) as VidInfoItem.VidHeaderItem
                val vidInfo = vidItem.vidInfo
                with(holder.itemView) {
                    binding1.titleTv.text = vidInfo.title
                    binding1.uploaderTv.text = vidInfo.uploader
                    binding1.uploaderTv.isSelected = true
                    binding1.viewsTv.text = vidInfo.viewCount?.toLongOrNull()?.let {
                        NumberUtils.format(it)
                    } ?: vidInfo.viewCount
                    binding1.likesTv.text = vidInfo.likeCount?.toLongOrNull()?.let {
                        NumberUtils.format(it)
                    } ?: vidInfo.likeCount
                    binding1.dislikesTv.text = vidInfo.dislikeCount?.toLongOrNull()?.let {
                        NumberUtils.format(it)
                    } ?: vidInfo.dislikeCount
                    binding1.uploadDateTv.text = vidInfo.uploadDate
                    vidInfo.duration.toLong().apply {
                        val minutes = TimeUnit.SECONDS.toMinutes(this)
                        val seconds = this - TimeUnit.MINUTES.toSeconds(minutes)
                        binding1.durationTv.text = context.getString(R.string.duration, minutes, seconds)
                    }
                }
            }
        }
    }

    inner class HeaderViewHolder(private val binding: VidHeaderBinding) : RecyclerView.ViewHolder(binding.root){
        val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        val uploaderTv: TextView = itemView.findViewById(R.id.uploader_tv)
        val views_tv: TextView = itemView.findViewById(R.id.views_tv)
        val likes_tvviews_tv: TextView = itemView.findViewById(R.id.likes_tv)
        val dislikes_tv: TextView = itemView.findViewById(R.id.dislikes_tv)
        val duration_tv: TextView = itemView.findViewById(R.id.duration_tv)
    }



    private fun shareUrl(url: String?, context: Context) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(context, Intent.createChooser(intent, null), null)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
         when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> {
                val layoutInflater = LayoutInflater.from(parent.context)
                binding1 = VidHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding1)

            }
            ITEM_VIEW_TYPE_ITEM -> {
                val layoutInflater = LayoutInflater.from(parent.context)
                binding2 = VidFormatBinding.inflate(layoutInflater, parent,false)
                return ViewHolder(binding2)
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is VidInfoItem.VidHeaderItem -> ITEM_VIEW_TYPE_HEADER
            is VidInfoItem.VidFormatItem -> ITEM_VIEW_TYPE_ITEM
        }
    }



//    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        companion object {
//            fun from(parent: ViewGroup): HeaderViewHolder {
//                val layoutInflater = LayoutInflater.from(parent.context)
////                val binding = VidHeaderBinding.inflate(layoutInflater)
//                val view = layoutInflater.inflate(R.layout.vid_header, parent, false)
//                return HeaderViewHolder(view)
//            }
//        }
//    }



}

class VidInfoDiffCallback : DiffUtil.ItemCallback<VidInfoItem>() {
    override fun areItemsTheSame(oldItem: VidInfoItem, newItem: VidInfoItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: VidInfoItem, newItem: VidInfoItem): Boolean {
        return oldItem == newItem
    }
}


class VidInfoListener(val clickListener: (VidInfoItem.VidFormatItem) -> Unit) {
    fun onClick(vidInfo: VidInfoItem.VidFormatItem) = clickListener(vidInfo)
}
