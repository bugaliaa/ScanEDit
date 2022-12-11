package com.example.editeditscanner.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.editeditscanner.R
import com.example.editeditscanner.data.Frame
import com.jsibbold.zoomage.ZoomageView

class ViewFrameAdapter(private val activity: Activity, private var frames: MutableList<Frame>) :
    PagerAdapter() {

    override fun getCount(): Int {
        return frames.size
    }

    fun get(index: Int): Frame {
        return frames[index]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    fun setFrames(frames: MutableList<Frame>) {
        this.frames = frames
    }

    private fun loadImage(uri: String?, imageView: ImageView) {
        Glide.with(activity).load(uri).diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true).into(imageView)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val frame = frames[position]

        @SuppressLint("InflateParams")
        val v = activity.layoutInflater.inflate(R.layout.row_page, null)
        val imageView: ZoomageView = v.findViewById(R.id.ssiv_page)
        when {
            frame.editedUri != null -> loadImage(frame.editedUri, imageView)
            frame.croppedUri != null -> loadImage(frame.croppedUri, imageView)
            else -> loadImage(frame.uri, imageView)
        }
        container.addView(v)
        return v
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}