package ru.garretech.readmanga.adapters

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import org.json.JSONArray
import com.github.piasy.biv.view.BigImageView
import ru.garretech.readmanga.interfaces.OnViewPagerClickListener


class ImageScrollAdapter(context : Context, imageList: JSONArray) : PagerAdapter() {
    val mContext by lazy { context }
    val mImageList by lazy { imageList }
    lateinit var onViewPagerClickListener : OnViewPagerClickListener

    fun setCustomOnClickListener(listener: OnViewPagerClickListener) {
        onViewPagerClickListener = listener
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return mImageList.length()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        if (container.getChildAt(position) == null) {
            val imageView = BigImageView(mContext)
            imageView.showImage(Uri.parse(mImageList.get(position).toString()))
            container.addView(imageView, position)

            imageView.setOnClickListener {
                onViewPagerClickListener.onClick()
            }

            return imageView
        } else
            return container.getChildAt(position)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

    }
}