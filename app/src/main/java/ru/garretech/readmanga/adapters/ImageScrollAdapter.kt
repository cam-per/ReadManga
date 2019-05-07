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
        val imageView = BigImageView(mContext)

        if (container.getChildAt(position) == null) {
            if (position > container.childCount) {
                for (index in container.childCount..position) {
                    imageView.showImage(Uri.parse(mImageList.get(index).toString()))
                    container.addView(imageView, index)
                    imageView.setOnClickListener {
                        onViewPagerClickListener.onClick()
                    }
                }
            } else {
                imageView.showImage(Uri.parse(mImageList.get(position).toString()))
                container.addView(imageView, position)

                imageView.setOnClickListener {
                    onViewPagerClickListener.onClick()
                }

            }
            return container.getChildAt(position)
        } else {
            return container.getChildAt(position)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

    }
}